package com.mgd.core.gradle

import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * S3 Upload Gradle task implementation.
 */
abstract class S3Upload extends AbstractS3Task {

    private String _sourceDirName
    private File _sourceDir

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
        return _sourceDirName
    }
    void setSourceDir(String sourceDir) {
        _sourceDirName = sourceDir
        _sourceDir = project.file(sourceDir)
    }

    @Input
    boolean overwrite = false

    @Input
    Integer minMultipartUploadThreshold = 100

    @TaskAction
    void task() {

        if (!bucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        TransferManager manager = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .withMultipartUploadThreshold((long) (minMultipartUploadThreshold * 1024 * 1025))
                .build()

        try {
            // directory upload
            if (_sourceDirName) {

                if (key || file) {
                    throw new GradleException('Invalid parameters: [key, file] are not valid for S3 Upload directory')
                }

                if (!keyPrefix) {
                    logger.quiet('Parameter [keyPrefix] was not provided: files will be uploaded to the S3 bucket root folder')
                }

                String destination = "s3://${bucket}${keyPrefix ? '/' + keyPrefix : ''}"
                logger.quiet("S3 Upload directory ${_sourceDirName} -> ${destination}")

                Transfer transfer = manager.uploadDirectory(bucket, keyPrefix, _sourceDir, true)

                S3Listener listener = new S3Listener(transfer, logger)
                transfer.addProgressListener(listener)
                transfer.waitForCompletion()
            }
            // single file upload
            else if (key && file) {

                if (keyPrefix) {
                    throw new GradleException('Invalid parameters: [keyPrefix] is not valid for S3 Upload single file')
                }

                String message = "S3 Upload ${file} -> s3://${bucket}/${key}"

                if (s3Client.doesObjectExist(bucket, key)) {
                    if (overwrite) {
                        message += ' with overwrite'
                    }
                    else {
                        logger.error("s3://${bucket}/${key} exists, not overwriting. If you want to overwrite a file you must specify overwrite = true in the task.")
                        return
                    }
                }

                logger.quiet(message)

                File f = new File(file)
                Upload up = manager.upload(bucket, key, f)
                S3Listener listener = new S3Listener(up, logger)
                up.addProgressListener(listener)
                up.addProgressListener(new AfterUploadListener(up, f, then))

                UploadResult result = up.waitForUploadResult()
                logger.info("S3 Upload completed: s3://${result.bucketName}/${result.key}")
            }
            else {
                throw new GradleException('Invalid parameters: one of [key, file] or [keyPrefix, sourceDir] pairs must be specified for S3 Upload')
            }
        }
        finally {
            manager.shutdownNow()
        }
    }
}
