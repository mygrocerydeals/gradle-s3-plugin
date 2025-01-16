package com.mgd.core.gradle

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload
import software.amazon.awssdk.transfer.s3.model.FileUpload
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest

/**
 * S3 Upload Gradle task implementation.
 */
abstract class S3Upload extends AbstractS3Task {

    private String source
    private File sourceDirectory

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
    String getSourceDir() {
        return source
    }

    void setSourceDir(String sourceDir) {
        source = sourceDir
        sourceDirectory = project.file(sourceDir)
    }

    @Input
    boolean overwrite = false

    @Input
    Integer minMultipartUploadThreshold = 100

    @TaskAction
    void task() {

        if (!taskBucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        S3TransferManager manager = S3TransferManager.builder()
            .s3Client(asyncS3Client)
            .build()

        // directory upload
        if (source) {

            if (key || file) {
                throw new GradleException('Invalid parameters: [key, file] are not valid for S3 Upload directory')
            }

            if (!keyPrefix) {
                logger.quiet('Parameter [keyPrefix] was not provided: files will be uploaded to the S3 bucket root folder')
            }

            String destination = "s3://${taskBucket}${keyPrefix ? '/' + keyPrefix : ''}"
            logger.quiet("S3 Upload directory ${source} -> ${destination}")

            UploadDirectoryRequest request = UploadDirectoryRequest.builder()
                .source(sourceDirectory.canonicalFile.toPath())
                .bucket(taskBucket)
                .s3Prefix(keyPrefix)
                .build()

            DirectoryUpload upload = manager.uploadDirectory(request)
            upload.completionFuture().join()

            return
        }

        // single file upload
        if (key && file) {

            if (keyPrefix) {
                throw new GradleException('Invalid parameters: [keyPrefix] is not valid for S3 Upload single file')
            }

            String message = "S3 Upload ${file} -> s3://${taskBucket}/${key}"

            try {
                s3Client.headObject(b -> b.bucket(taskBucket).key(key))
                if (overwrite) {
                    message += ' with overwrite'
                }
                else {
                    logger.error("s3://${taskBucket}/${key} exists, not overwriting. If you want to overwrite a file you must specify overwrite = true in the task.")
                    return
                }
            }
            catch (NoSuchKeyException ignored) {
                // do nothing
            }

            logger.quiet(message)

            File f = new File(file)
            AfterTransferListener transferListener = null

            if (then) {
                transferListener = new AfterTransferListener(f, logger, then)
            }

            UploadFileRequest request = UploadFileRequest.builder()
                .source(f.canonicalFile.toPath())
                .putObjectRequest(b -> b.bucket(taskBucket).key(key))
                .addTransferListener(new S3Listener(logger, transferListener))
                .build()

            FileUpload upload = manager.uploadFile(request)
            upload.completionFuture().join()

            return
        }

        // default action: invalid request
        throw new GradleException('Invalid parameters: one of [key, file] or [keyPrefix, sourceDir] pairs must be specified for S3 Upload')
    }
}
