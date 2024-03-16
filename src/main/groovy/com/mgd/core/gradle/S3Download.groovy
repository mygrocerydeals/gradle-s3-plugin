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

                DownloadDirectoryRequest directoryRequest = getRecursiveDirectoryRequest(dir, taskBucket, keyPrefix)
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

            DownloadFileRequest fileRequest = getSingleFileRequest(file, taskBucket, key, then)
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
        String path = parsePathPattern(pattern)
        List<String> segments = path.split(/\//)
        int segmentCount = segments.size()

        // create the parent directory if it doesn't already exist
        (parentDir.canonicalFile).mkdirs()

        DownloadFilter filter = new DownloadFilter() {

            @Override
            boolean test(S3Object s3Object) {

                List<String> s3Segments = s3Object.key().split(/\//)
                int s3SegmentCount = s3Segments.size() - 1

                if (!s3SegmentCount || (s3SegmentCount < segmentCount)) {
                    // file is higher in the hierarchy than the search pattern depth
                    return false
                }

                s3Segments.removeLast()
                String s3Path = s3Segments.join('/')

                if (isWildcard) {
                    // matcher for wildcard expression
                    return s3Path.startsWith(path)
                }

                // matcher for directory name expression
                for (int i = 0; i < segmentCount; i++) {
                    if (s3Segments[i] != segments[i]) {
                        return false
                    }
                }

                // all search pattern segments match the s3 parent segments
                return true
            }
        }

        DownloadDirectoryRequest directoryRequest = DownloadDirectoryRequest.builder()
                .bucket(bucket)
                .destination(parentDir.canonicalFile.toPath())
                .filter(filter)
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
        String pathPattern = isTree ? segments.join('/') : ''

        // create the parent directory if it doesn't already exist
        (parentDir.canonicalFile).mkdirs()

        DownloadFilter filter = new DownloadFilter() {

            @Override
            boolean test(S3Object s3Object) {

                List<String> s3Segments = s3Object.key().split(/\//)
                boolean isS3Tree = (s3Segments.size() > 1)
                String s3Name = isS3Tree ? s3Segments.removeLast() : s3Segments.first()
                String s3Path = isS3Tree ? s3Segments.join('/') : ''

                if (isTree && !s3Path.startsWith(pathPattern)) {
                    return false
                }

                return s3Name.startsWith(filePattern)
            }
        }

        DownloadDirectoryRequest.Builder builder = DownloadDirectoryRequest.builder()
                .bucket(bucket)
                .destination(parentDir.canonicalFile.toPath())
                .filter(filter)

        return builder.build()
    }

    /**
     * Helper method to generate a DownloadFileRequest for the file path pattern expression.
     */
    protected DownloadFileRequest getPathPatternFileRequest(File parentDir, String bucket, String pattern, Closure<Void> then) {

        File f = new File(parentDir, pattern)

        return getSingleFileRequest(f, bucket, pattern, then)
    }

    /**
     * Helper method to generate a DownloadDirectoryRequest for the directory key.
     */
    protected DownloadDirectoryRequest getRecursiveDirectoryRequest(File parentDir, String bucket, String key) {

        // create the parent directory if it doesn't already exist
        (parentDir.canonicalFile).mkdirs()

        DownloadDirectoryRequest.Builder builder = DownloadDirectoryRequest.builder()
                .bucket(bucket)
                .destination(parentDir.canonicalFile.toPath())

        if (key) {
            builder.listObjectsV2RequestTransformer(l -> l.prefix(key).delimiter('/'))
        }
        else {
            logger.quiet('Parameter [keyPrefix] was not provided: the entire S3 bucket contents will be downloaded')
        }

        return builder.build()
    }

    /**
     * Overloaded method to generate a DownloadFileRequest for the file destination and key.
     */
    protected DownloadFileRequest getSingleFileRequest(String file, String bucket, String key, Closure<Void> then) {

        File f = new File(file)

        return getSingleFileRequest(f, bucket, key, then)
    }

    /**
     * Helper method to generate a DownloadFileRequest for the file destination and key.
     */
    protected DownloadFileRequest getSingleFileRequest(File file, String bucket, String key, Closure<Void> then) {

        if (file.parentFile) {
            (file.parentFile.canonicalFile).mkdirs()
        }

        AfterTransferListener transferListener = null
        if (then) {
            transferListener = new AfterTransferListener(file, logger, then)
        }

        DownloadFileRequest fileRequest = DownloadFileRequest.builder()
                .getObjectRequest(b -> b.bucket(bucket).key(key))
                .destination(file.canonicalFile.toPath())
                .addTransferListener(new S3Listener(logger, transferListener))
                .build()

        return fileRequest
    }
}
