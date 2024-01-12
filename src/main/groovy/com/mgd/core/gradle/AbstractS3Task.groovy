package com.mgd.core.gradle

import com.amazonaws.auth.*
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder
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

    private static final String BUCKET = 'bucket'
    private static final String PROFILE = 'profile'
    private static final String ENDPOINT = 'endpoint'
    private static final String REGION = 'region'

    @Optional
    @Input
    String bucket

    String getBucket() {
        return bucket ?: getS3Property(BUCKET)
    }

    @Optional
    @Input
    String profile

    String getProfile() {
        return profile ?: getS3Property(PROFILE)
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

        String region = getS3Property(REGION)
        if (region) {
            String endpoint = getS3Property(ENDPOINT)
            if (endpoint) {
                builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
            } else {
                builder.withRegion(region)
            }
        }

        return builder.build()
    }

    abstract void task()

    private String getS3Property(String name) {

        switch (name) {
            case BUCKET:
                return S3Extension.properties.bucket
            case PROFILE:
                return S3Extension.properties.profile
            case REGION:
                return S3Extension.properties.region
            case ENDPOINT:
                return S3Extension.properties.endpoint
            default:
                return null
        }
    }
}
