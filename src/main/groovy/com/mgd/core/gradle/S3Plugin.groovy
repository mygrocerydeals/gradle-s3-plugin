package com.mgd.core.gradle

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional

import java.text.DecimalFormat
import org.gradle.api.logging.Logger

class S3Extension {

    String profile
    String region
    String bucket
}

abstract class S3Task extends DefaultTask {

    @Optional
    @Input
    String bucket

    String getBucket() {
        return bucket ?: project.s3.bucket
    }

    @Optional
    @Input
    String profile

    String getProfile() {
        return profile ?: project.s3.profile
    }

    @Internal
    AmazonS3 getS3Client() {

        ProfileCredentialsProvider profileCreds
        if (profile) {
            logger.quiet("Using AWS credentials profile: ${profile}")
            profileCreds = new ProfileCredentialsProvider(profile)
        }
        else {
            profileCreds = new ProfileCredentialsProvider()
        }

        AWSCredentialsProvider creds = new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                profileCreds,
                new EC2ContainerCredentialsProviderWrapper()
        )

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                                            .withCredentials(creds)

        String region = project.s3.region
        if (region) {
            builder.withRegion(region)
        }

        return builder.build()
    }
}


class S3Upload extends S3Task {

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
    String sourceDir

    @Input
    boolean overwrite = false

    @TaskAction
    void task() {

        if (!bucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        // directory upload
        if (sourceDir) {

            if (key || file) {
                throw new GradleException('Invalid parameters: [key, file] are not valid for S3 Upload directory')
            }

            if (!keyPrefix) {
                logger.quiet('Parameter [keyPrefix] was not provided: files will be uploaded to the S3 bucket root folder')
            }

            String destination = "s3://${bucket}${keyPrefix ? '/' + keyPrefix : ''}"
            logger.quiet("S3 Upload directory ${project.file(sourceDir)}/ → ${destination}")

            TransferManager manager = TransferManagerBuilder.newInstance().withS3Client(s3Client).build()
            try {
                Transfer transfer = manager.uploadDirectory(bucket, keyPrefix, project.file(sourceDir), true)

                S3Listener listener = new S3Listener(transfer, logger)
                transfer.addProgressListener(listener)
                transfer.waitForCompletion()
            } finally {
                manager.shutdownNow()
            }
        }

        // single file upload
        else if (key && file) {

            if (keyPrefix) {
                throw new GradleException('Invalid parameters: [keyPrefix] is not valid for S3 Upload single file')
            }

            if (s3Client.doesObjectExist(bucket, key)) {
                if (overwrite) {
                    logger.quiet("S3 Upload ${file} → s3://${bucket}/${key} with overwrite")
                    s3Client.putObject(bucket, key, new File(file))
                } else {
                    logger.quiet("s3://${bucket}/${key} exists, not overwriting")
                }
            } else {
                logger.quiet("S3 Upload ${file} → s3://${bucket}/${key}")
                s3Client.putObject(bucket, key, new File(file))
            }
        } else {
            throw new GradleException('Invalid parameters: one of [key, file] or [keyPrefix, sourceDir] pairs must be specified for S3 Upload')
        }
    }
}


class S3Download extends S3Task {

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
    String destDir

    @TaskAction
    void task() {

        Transfer transfer

        if (!bucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        TransferManager manager = TransferManagerBuilder.newInstance().withS3Client(s3Client).build()
        try {
            // directory download
            if (destDir) {

                if (key || file) {
                    throw new GradleException('Invalid parameters: [key, file] are not valid for S3 Download recursive')
                }

                if (!keyPrefix) {
                    logger.quiet('Parameter [keyPrefix] was not provided: the entire S3 bucket contents will be downloaded')
                }

                String source = "s3://${bucket}${keyPrefix ? '/' + keyPrefix : ''}"
                logger.quiet("S3 Download recursive ${source} → ${project.file(destDir)}/")

                transfer = manager.downloadDirectory(bucket, keyPrefix, project.file(destDir))
            }

            // single file download
            else if (key && file) {

                if (keyPrefix) {
                    throw new GradleException('Invalid parameters: [keyPrefix] is not valid for S3 Download single file')
                }

                logger.quiet("S3 Download s3://${bucket}/${key} → ${file}")
                File f = new File(file)
                f.parentFile.mkdirs()
                transfer = manager.download(bucket, key, f)
            } else {
                throw new GradleException('Invalid parameters: one of [key, file] or [keyPrefix, destDir] pairs must be specified for S3 Download')
            }

            S3Listener listener = new S3Listener(transfer, logger)
            transfer.addProgressListener(listener)
            transfer.waitForCompletion()
        } finally {
            manager.shutdownNow()
        }
    }
}


class S3Listener implements ProgressListener {

    DecimalFormat df = new DecimalFormat('#0.0')
    Transfer transfer
    Logger logger

    S3Listener(Transfer transfer, Logger logger) {
        this.transfer = transfer
        this.logger = logger
    }

    void progressChanged(ProgressEvent e) {
        logger.info("${df.format(transfer.progress.percentTransferred)}%")
    }
}


class S3Plugin implements Plugin<Project> {

    void apply(Project target) {
        target.extensions.create('s3', S3Extension)
    }
}
