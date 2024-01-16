package com.mgd.core.gradle

import com.amazonaws.auth.*
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

/**
 * Abstract base class for the S3Upload and S3Download S3 Gradle plugin tasks.
 */
abstract class AbstractS3Task extends DefaultTask {

    // common Gradle task properties with default values in S3 Extension
    @Optional
    @Input
    String bucket

    @Optional
    @Input
    String profile

    @Optional
    @Input
    String endpoint

    @Optional
    @Input
    String region

    @Internal
    Closure<Void> then

    // entry point for Gradle task
    abstract void task()

    // property getters used by tasks
    @Internal
    protected String getTaskBucket() {
        return bucket ?: getS3Property(BUCKET)
    }

    @Internal
    protected String getTaskProfile() {
        return profile ?: getS3Property(PROFILE)
    }

    @Internal
    protected String getTaskRegion() {
        return region ?: getS3Property(REGION)
    }

    @Internal
    protected String getTaskEndpoint() {
        return endpoint ?: getS3Property(ENDPOINT)
    }

    @Internal
    protected AmazonS3 getS3Client() {

        ProfileCredentialsProvider profileCreds
        if (taskProfile) {
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

        if (taskRegion) {
            if (taskEndpoint) {
                builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(taskEndpoint, taskRegion))
            }
            else {
                builder.withRegion(taskRegion)
            }
        }
        else if (taskEndpoint) {
            throw new GradleException('Invalid parameters: [endpoint] is not valid without a provided [region]')
        }

        return builder.build()
    }

    // S3 Extension property names
    private static final String BUCKET = 'bucket'
    private static final String PROFILE = 'profile'
    private static final String ENDPOINT = 'endpoint'
    private static final String REGION = 'region'

    // helper method to return a named S3 Extension property
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
