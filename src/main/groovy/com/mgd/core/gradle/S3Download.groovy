package com.mgd.core.gradle

import com.amazonaws.services.s3.transfer.Download
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
class S3Download extends AbstractS3Task {

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
    String destDir

    @TaskAction
    void task() {

        List<Transfer> transfers

        if (!bucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        TransferManager manager = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .build()

        try {
            // directory download
            if (destDir) {

                if (key || file) {
                    String param = key ? 'key' : 'file'
                    throw new GradleException("Invalid parameters: [${param}] is not valid for S3 Download recursive")
                }

                if (pathPatterns) {
                    if (keyPrefix) {
                        throw new GradleException('Invalid parameters: [pathPatterns] cannot be combined with [keyPrefix] for S3 Download recursive')
                    }

                    logger.quiet("S3 Download path patterns s3://${bucket}/${pathPatterns.join(',')} -> ${destDir}")

                    transfers = pathPatterns.collect { String pattern ->

                        if (pattern.contains('*') || pattern.endsWith('/')) {
                            String prefix = pattern.replaceAll(/\*$/, '')
                            Transfer t = manager.downloadDirectory(bucket, prefix, project.file(destDir))
                            return t
                        }

                        File f = project.file("${destDir}/${pattern}")
                        f.parentFile.mkdirs()
                        return manager.download(bucket, pattern, f)
                    }
                }
                else {
                    if (!keyPrefix) {
                        logger.quiet('Parameter [keyPrefix] was not provided: the entire S3 bucket contents will be downloaded')
                    }

                    String source = "s3://${bucket}${keyPrefix ? '/' + keyPrefix : ''}"
                    logger.quiet("S3 Download recursive ${source} -> ${destDir}")

                    transfers = [manager.downloadDirectory(bucket, keyPrefix, project.file(destDir))]
                }
            }
            // single file download
            else if (key && file) {

                if (keyPrefix || pathPatterns) {
                    String param = keyPrefix ? 'keyPrefix' : 'pathPatterns'
                    throw new GradleException("Invalid parameters: [${param}] is not valid for S3 Download single file")
                }

                logger.quiet("S3 Download s3://${bucket}/${key} -> ${file}")

                File f = new File(file)
                f.parentFile.mkdirs()
                transfers = [manager.download(bucket, key, f)]
            }
            else {
                throw new GradleException('Invalid parameters: one of [key, file], [keyPrefix, destDir] or [pathPatterns, destDir] pairs must be specified for S3 Download')
            }

            transfers.each { Transfer transfer ->
                S3Listener listener = new S3Listener(transfer, logger)
                transfer.addProgressListener(listener)
                if (transfer.class == Download) {
                    transfer.addProgressListener(new AfterDownloadListener((Download)transfer, project.file(destDir), then))
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
