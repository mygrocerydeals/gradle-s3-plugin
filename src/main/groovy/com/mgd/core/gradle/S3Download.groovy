package com.mgd.core.gradle

import software.amazon.awssdk.transfer.s3.model.Download
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * S3 Download Gradle task implementation.
 */
abstract class S3Download extends AbstractS3Task {

    private String destination
    private File destinationDirectory

    @Optional
    @Input
    String key

    @Optional
    @Input
    String file

    @Optional
    @Input
    String keyPrefix

    @Optional
    @Input
    Iterable<String> pathPatterns

    @Optional
    @OutputDirectory
    String getDestDir() {
        return destination
    }
    void setDestDir(String destDir) {
        destination = destDir
        destinationDirectory = project.file(destDir)
    }

    @TaskAction
    void task() {

        List<Transfer> transfers

        if (!taskBucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        TransferManager manager = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .build()

        try {
            // directory download
            if (destination) {

                if (key || file) {
                    String param = key ? 'key' : 'file'
                    throw new GradleException("Invalid parameters: [${param}] is not valid for S3 Download recursive")
                }

                if (pathPatterns) {
                    if (keyPrefix) {
                        throw new GradleException('Invalid parameters: [pathPatterns] cannot be combined with [keyPrefix] for S3 Download recursive')
                    }

                    logger.quiet("S3 Download path patterns s3://${taskBucket}/${pathPatterns.join(',')} -> ${destination}")

                    // need a local value here because Groovy somehow loses the ref to destinationDirectory in the collect{} closure
                    File dir = destinationDirectory
                    transfers = pathPatterns.collect { String pattern ->

                        if (pattern.contains('*') || pattern.endsWith('/')) {
                            String prefix = pattern.replaceAll(/\*$/, '')
                            Transfer t = manager.downloadDirectory(taskBucket, prefix, dir)
                            return t
                        }

                        File f = new File(dir, pattern)
                        f.parentFile.mkdirs()
                        return manager.download(taskBucket, pattern, f)
                    }
                }
                else {
                    if (!keyPrefix) {
                        logger.quiet('Parameter [keyPrefix] was not provided: the entire S3 bucket contents will be downloaded')
                    }

                    String source = "s3://${taskBucket}${keyPrefix ? '/' + keyPrefix : ''}"
                    logger.quiet("S3 Download recursive ${source} -> ${destination}")

                    transfers = [manager.downloadDirectory(taskBucket, keyPrefix, destinationDirectory)]
                }
            }
            // single file download
            else if (key && file) {

                if (keyPrefix || pathPatterns) {
                    String param = keyPrefix ? 'keyPrefix' : 'pathPatterns'
                    throw new GradleException("Invalid parameters: [${param}] is not valid for S3 Download single file")
                }

                logger.quiet("S3 Download s3://${taskBucket}/${key} -> ${file}")

                File f = new File(file)
                f.parentFile.mkdirs()
                transfers = [manager.download(taskBucket, key, f)]
            }
            else {
                throw new GradleException('Invalid parameters: one of [key, file], [keyPrefix, destDir] or [pathPatterns, destDir] pairs must be specified for S3 Download')
            }

            transfers.each { Transfer transfer ->
                S3Listener listener = new S3Listener(transfer, logger)
                transfer.addProgressListener(listener)
                if (transfer.class == Download) {
                    transfer.addProgressListener(new AfterDownloadListener((Download)transfer, destinationDirectory, then))
                }
            }
            transfers.each { Transfer transfer ->
                transfer.waitForCompletion()
            }
        }
        finally {
            manager.shutdownNow()
        }
    }
}
