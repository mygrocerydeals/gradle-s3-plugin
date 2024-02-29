package com.mgd.core.gradle

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.config.DownloadFilter
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.Transfer

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
        AfterTransferListener transferListener = null

        if (!taskBucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        S3TransferManager manager = S3TransferManager.builder()
                                        .s3Client(asyncS3Client)
                                        .build()

        // directory download
        if (destination) {

            File dir = destinationDirectory

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
                transfers = pathPatterns.collect { String pattern ->

                    // recursive directory download
                    if (pattern.endsWith('/')) {

                        boolean isWildcard = pattern.endsWith('*/')
                        String key = parsePathPattern(pattern)
                        File f = new File(dir, key)

                        // create any parent directories which prefix the wildcard expression
                        if (isWildcard) {
                            List<String> segments = key.split(/\//)
                            if (segments.size() > 1) {
                                segments.removeLast()
                                f = new File(dir, segments.join('/'))
                            }
                        }

                        (f.canonicalFile).mkdirs()

                        DownloadDirectoryRequest directoryRequest = DownloadDirectoryRequest.builder()
                                .bucket(taskBucket)
                                .listObjectsV2RequestTransformer(l -> l.prefix(key).delimiter('/'))
                                .destination(f.toPath())
                                .build()

                        return manager.downloadDirectory(directoryRequest)
                    }

                    // file pattern download
                    if (pattern.endsWith('*')) {

                        List<String> segments = parsePathPattern(pattern).split(/\//)
                        boolean isTree = (segments.size() > 1)
                        String filePattern = isTree ? segments.removeLast() : segments.first()
                        String key = isTree ? segments.join('/') : null

                        File f = isTree ? new File(dir, key) : dir
                        (f.canonicalFile).mkdirs()

                        DownloadFilter filter = new DownloadFilter() {
                            @Override
                            boolean test(S3Object s3Object) {
                                String name = s3Object.key().split(/\//).last()
                                return name.startsWith(filePattern)
                            }
                        }

                        DownloadDirectoryRequest.Builder directoryRequest = DownloadDirectoryRequest.builder()
                                .bucket(taskBucket)
                                .destination(f.toPath())
                                .filter(filter)

                        if (isTree) {
                            directoryRequest.listObjectsV2RequestTransformer(l -> l.prefix(key).delimiter('/'))
                        }

                        return manager.downloadDirectory(directoryRequest.build())
                    }

                    // single file download
                    String key = pattern
                    File f = new File(dir, pattern)
                    if (f.parentFile) {
                        (f.parentFile.canonicalFile).mkdirs()
                    }

                    if (then) {
                        transferListener = new AfterTransferListener(f, then)
                    }

                    DownloadFileRequest fileRequest = DownloadFileRequest.builder()
                            .getObjectRequest(b -> b.bucket(taskBucket).key(key))
                            .destination(f.toPath())
                            .addTransferListener(new S3Listener(logger, transferListener))
                            .build()

                    return manager.downloadFile(fileRequest)
                }
            }
            else {
                // recursive directory download

                parseKey(keyPrefix)

                String source = "s3://${taskBucket}${keyPrefix ? '/' + keyPrefix : ''}"
                logger.quiet("S3 Download recursive ${source} -> ${destination}")

                File f = keyPrefix ? new File(dir, keyPrefix) : dir
                f.mkdirs()

                DownloadDirectoryRequest.Builder builder = DownloadDirectoryRequest.builder()
                        .bucket(taskBucket)
                        .destination(f.toPath())

                if (keyPrefix) {
                    builder.listObjectsV2RequestTransformer(l -> l.prefix(keyPrefix).delimiter('/'))
                }
                else {
                    logger.quiet('Parameter [keyPrefix] was not provided: the entire S3 bucket contents will be downloaded')
                }

                transfers = [manager.downloadDirectory(builder.build())]
            }
        }
        // single file download
        else if (key && file) {

            parseKey(key)
            File f = new File(file)

            if (keyPrefix || pathPatterns) {
                String param = keyPrefix ? 'keyPrefix' : 'pathPatterns'
                throw new GradleException("Invalid parameters: [${param}] is not valid for S3 Download single file")
            }

            logger.quiet("S3 Download s3://${taskBucket}/${key} -> ${file}")

            if (f.parentFile) {
                (f.parentFile.canonicalFile).mkdirs()
            }

            if (then) {
                transferListener = new AfterTransferListener(f, then)
            }

            DownloadFileRequest request = DownloadFileRequest.builder()
                                            .getObjectRequest(b -> b.bucket(taskBucket).key(key))
                                            .destination(f.toPath())
                                            .addTransferListener(new S3Listener(logger, transferListener))
                                            .build()

            transfers = [manager.downloadFile(request)]
        }
        else {
            throw new GradleException('Invalid parameters: one of [key, file], [keyPrefix, destDir] or [pathPatterns, destDir] pairs must be specified for S3 Download')
        }

        transfers.each { Transfer transfer ->
            transfer.completionFuture().join()
        }
    }
}
