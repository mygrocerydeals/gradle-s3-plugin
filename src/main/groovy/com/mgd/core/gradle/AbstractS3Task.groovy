package com.mgd.core.gradle

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

/**
 * Abstract base class for the S3Upload and S3Download S3 Gradle plugin tasks.
 */
abstract class AbstractS3Task extends DefaultTask {

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
    Closure<Void> then

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

    abstract void task()
}
