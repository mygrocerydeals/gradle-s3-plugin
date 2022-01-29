package com.fuseanalytics.gradle.s3

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.Download
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
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

    @Internal
    Closure<Void> then

    String getBucket() { bucket ?: project.s3.bucket }

    @Internal
    AmazonS3 getS3Client() {

        def profileCreds
        if (project.s3.profile) {
            logger.quiet("Using AWS credentials profile: ${project.s3.profile}")
            profileCreds = new ProfileCredentialsProvider(project.s3.profile)
        }
        else {
            profileCreds = new ProfileCredentialsProvider()
        }
        def creds = new AWSCredentialsProviderChain(
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
        builder.build()
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

    @Input
    Integer minMultipartUploadThreshold = 100
    
    @TaskAction
    def task() {

        if (!bucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        TransferManager manager = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .withMultipartUploadThreshold((long) (minMultipartUploadThreshold * 1024 * 1025))
                .build()
        try {
            // directory upload
            if (keyPrefix && sourceDir) {

                if (key || file) {
                    throw new GradleException('Invalid parameters: [key, file] are not valid for S3 Upload directory')
                }

                logger.quiet("S3 Upload directory ${project.file(sourceDir)}/ → s3://${bucket}/${keyPrefix}")

                Transfer transfer = manager.uploadDirectory(bucket, keyPrefix, project.file(sourceDir), true)

                for( Upload u : transfer.getSubTransfers() ) {
                    u.addProgressListener( new AfterUploadListener( u, project.file(sourceDir), then ) )
                }

                S3Listener listener = new S3Listener(transfer, logger)
                transfer.addProgressListener(listener)
                transfer.waitForCompletion()
            }
            // single file upload
            else if (key && file) {
                Upload up = null
                if (s3Client.doesObjectExist(bucket, key)) {
                    if (overwrite) {
                        logger.quiet("S3 Upload ${file} → s3://${bucket}/${key} with overwrite")
                        up = manager.upload(bucket, key, new File(file))
                    } else {
                        logger.quiet("s3://${bucket}/${key} exists, not overwriting")
                    }
                } else {
                    logger.quiet("S3 Upload ${file} → s3://${bucket}/${key}")
                    up = manager.upload(bucket, key, new File(file))
                }
                S3Listener listener = new S3Listener(up, logger)
                up.addProgressListener(listener)
                up.addProgressListener(new AfterUploadListener(up, project.file(file), then))
                UploadResult result = up.waitForUploadResult()
                logger.info("S3 Upload completed: s3://${result.bucketName}/${result.key}")
            } else {
                throw new GradleException('Invalid parameters: one of [key, file] or [keyPrefix, sourceDir] pairs must be specified for S3 Upload')
            }
        } finally {
            manager.shutdownNow()
        }
    }
}


class S3Download extends S3Task {

    @Optional
    @Input
    @Deprecated
    String key

    @Optional
    @Input
    Iterable<String> keys

    @Optional
    @OutputFile
    @Deprecated
    String file

    @Optional
    @Input
    @Deprecated
    String keyPrefix

    @Optional
    @OutputDirectory
    String destDir

    @TaskAction
    def task() {

        List<Transfer> transfers

        if (!bucket) {
            throw new GradleException('Invalid parameters: [bucket] was not provided and/or a default was not set')
        }

        TransferManager manager = TransferManagerBuilder.newInstance().withS3Client(s3Client).build()
        try {
            // directory download
            if (keyPrefix && destDir) {
                if (key || file || keys) {
                    throw new GradleException('Invalid parameters: [key, file] are not valid for S3 Download recursive')
                }
                logger.quiet("S3 Download recursive s3://${bucket}/${keyPrefix} → ${project.file(destDir)}/")
                transfers = [ manager.downloadDirectory(bucket, keyPrefix, project.file(destDir)) ]

                for( Download u : transfers.first().getSubTransfers() ) {
                    u.addProgressListener( new AfterDownloadListener( u, project.file(destDir), then ) )
                }
            }
            // single file download
            else if (key && file) {
                if (keyPrefix || destDir) {
                    throw new GradleException('Invalid parameters: [keyPrefix, destDir] are not valid for S3 Download single file')
                }
                logger.quiet("S3 Download s3://${bucket}/${key} → ${file}")
                File f = new File(file)
                f.parentFile.mkdirs()
                transfers = [ manager.download(bucket, key, f) ]
            }
            else if( keys && destDir) {
                if( keyPrefix || key || file ) {
                    throw new GradleException("Invalid parameters: [keyPrefix, key, file] are not valid for S3 Download using multiple keys syntax")
                }
                logger.quiet("S3 Download s3://${bucket}/${keys.join(",")} → ${destDir}")
                transfers = keys.collect { String key ->
                    if( key.contains('*') || key.endsWith('/') ) {
                        String prefix = key.replaceAll( /\*$/, '')
                        Transfer t = manager.downloadDirectory(bucket, prefix, project.file(destDir) )
                        for( Download u : t.getSubTransfers() ) {
                            u.addProgressListener( new AfterDownloadListener(u, project.file(destDir), then) )
                        }
                    } else {
                        File output = project.file("${destDir}/${key}")
                        output.parentFile.mkdirs()
                        return manager.download( bucket, key, output )
                    }
                }
            } else {
                throw new GradleException('Invalid parameters: one of [key, file] or [keyPrefix, destDir] pairs must be specified for S3 Download')
            }

            transfers.each { Transfer transfer ->
                def listener = new S3Listener(transfer, logger)
                transfer.addProgressListener(listener)
                transfer.addProgressListener( new AfterDownloadListener((Download)transfer, project.file(destDir), then) )
            }
            transfers.each { Transfer transfer ->
                transfer.waitForCompletion()
            }
        } finally {
            manager.shutdownNow()
        }
    }
}


class S3Listener implements ProgressListener {

    DecimalFormat df = new DecimalFormat("#0.0")
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
