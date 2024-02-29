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

        if (!taskBucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        S3TransferManager manager = S3TransferManager.builder()
                                        .s3Client(asyncS3Client)
                                        .build()

        List<Transfer> transfers

        // directory download
        if (destination) {

            // need a local value here because Groovy somehow loses the ref to destinationDirectory in the collect{} closure
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

                transfers = pathPatterns.collect { String pattern ->

                    // recursive directory download
                    if (pattern.endsWith('/')) {

                        DownloadDirectoryRequest directoryRequest = getPathPatternDirectoryRequest(dir, taskBucket, pattern)
                        return manager.downloadDirectory(directoryRequest)
                    }

                    // file pattern download
                    if (pattern.endsWith('*')) {

                        DownloadDirectoryRequest directoryRequest = getWildcardDirectoryRequest(dir, taskBucket, pattern)
                        return manager.downloadDirectory(directoryRequest)
                    }

                    // otherwise, treat as a single file download
                    DownloadFileRequest fileRequest = getPathPatternFileRequest(dir, taskBucket, pattern, then)
                    return manager.downloadFile(fileRequest)
                }
            }
            else {
                // recursive directory download
                parseKey(keyPrefix)

                String source = "s3://${taskBucket}${keyPrefix ? '/' + keyPrefix : ''}"
                logger.quiet("S3 Download recursive ${source} -> ${destination}")

                DownloadDirectoryRequest directoryRequest = getRecursiveDirectoryRequest(dir, keyPrefix, taskBucket)
                transfers = [manager.downloadDirectory(directoryRequest)]
            }
        }
        // single file download
        else if (key && file) {

            parseKey(key)

            if (keyPrefix || pathPatterns) {
                String param = keyPrefix ? 'keyPrefix' : 'pathPatterns'
                throw new GradleException("Invalid parameters: [${param}] is not valid for S3 Download single file")
            }

            logger.quiet("S3 Download s3://${taskBucket}/${key} -> ${file}")

            DownloadFileRequest fileRequest = getSingleFileRequest(file, key, taskBucket, then)
            transfers = [manager.downloadFile(fileRequest)]
        }
        else {
            throw new GradleException('Invalid parameters: one of [key, file], [keyPrefix, destDir] or [pathPatterns, destDir] pairs must be specified for S3 Download')
        }

        transfers.each { Transfer transfer ->
            transfer.completionFuture().join()
        }
    }

    /**
     * Helper method to generate a DownloadDirectoryRequest for the directory path pattern expression.
     */
    protected DownloadDirectoryRequest getPathPatternDirectoryRequest(File parentDir, String bucket, String pattern) {

        boolean isWildcard = pattern.endsWith('*/')
        String key = parsePathPattern(pattern)
        File f = new File(parentDir, key)

        // create any parent directories which prefix the wildcard expression
        if (isWildcard) {
            List<String> segments = key.split(/\//)
            if (segments.size() > 1) {
                segments.removeLast()
                f = new File(parentDir, segments.join('/'))
            }
        }
        (f.canonicalFile).mkdirs()

        DownloadDirectoryRequest directoryRequest = DownloadDirectoryRequest.builder()
                .bucket(bucket)
                .listObjectsV2RequestTransformer(l -> l.prefix(key).delimiter('/'))
                .destination(f.toPath())
                .build()

        return directoryRequest
    }

    /**
     * Helper method to generate a DownloadDirectoryRequest for the wildcard path pattern expression.
     */
    protected DownloadDirectoryRequest getWildcardDirectoryRequest(File parentDir, String bucket, String pattern) {

        List<String> segments = parsePathPattern(pattern).split(/\//)
        boolean isTree = (segments.size() > 1)
        String filePattern = isTree ? segments.removeLast() : segments.first()
        String key = isTree ? segments.join('/') : null

        File f = isTree ? new File(parentDir, key) : parentDir
        (f.canonicalFile).mkdirs()

        DownloadFilter filter = new DownloadFilter() {

            @Override
            boolean test(S3Object s3Object) {
                String name = s3Object.key().split(/\//).last()
                return name.startsWith(filePattern)
            }
        }

        DownloadDirectoryRequest.Builder builder = DownloadDirectoryRequest.builder()
                .bucket(bucket)
                .destination(f.toPath())
                .filter(filter)

        if (isTree) {
            builder.listObjectsV2RequestTransformer(l -> l.prefix(key).delimiter('/'))
        }

        return builder.build()
    }

    /**
     * Helper method to generate a DownloadFileRequest for the file path pattern expression.
     */
    protected DownloadFileRequest getPathPatternFileRequest(File parentDir, String bucket, String pattern, Closure<Void> then) {

        String key = pattern
        File f = new File(parentDir, pattern)
        if (f.parentFile) {
            (f.parentFile.canonicalFile).mkdirs()
        }

        AfterTransferListener transferListener = null
        if (then) {
            transferListener = new AfterTransferListener(f, then)
        }

        DownloadFileRequest fileRequest = DownloadFileRequest.builder()
                .getObjectRequest(b -> b.bucket(bucket).key(key))
                .destination(f.toPath())
                .addTransferListener(new S3Listener(logger, transferListener))
                .build()

        return fileRequest
    }

    /**
     * Helper method to generate a DownloadDirectoryRequest for the directory key.
     */
    protected DownloadDirectoryRequest getRecursiveDirectoryRequest(File parentDir, String key, String bucket) {

        File f = key ? new File(parentDir, key) : parentDir
        f.mkdirs()

        DownloadDirectoryRequest.Builder builder = DownloadDirectoryRequest.builder()
                .bucket(bucket)
                .destination(f.toPath())

        if (key) {
            builder.listObjectsV2RequestTransformer(l -> l.prefix(key).delimiter('/'))
        }
        else {
            logger.quiet('Parameter [keyPrefix] was not provided: the entire S3 bucket contents will be downloaded')
        }

        return builder.build()
    }

    /**
     * Helper method to generate a DownloadFileRequest for the file destination and key.
     */
    protected DownloadFileRequest getSingleFileRequest(String file, String key, String bucket, Closure<Void> then) {

        File f = new File(file)
        if (f.parentFile) {
            (f.parentFile.canonicalFile).mkdirs()
        }

        AfterTransferListener transferListener = null
        if (then) {
            transferListener = new AfterTransferListener(f, then)
        }

        DownloadFileRequest fileRequest = DownloadFileRequest.builder()
                .getObjectRequest(b -> b.bucket(bucket).key(key))
                .destination(f.toPath())
                .addTransferListener(new S3Listener(logger, transferListener))
                .build()

        return fileRequest
    }
}
