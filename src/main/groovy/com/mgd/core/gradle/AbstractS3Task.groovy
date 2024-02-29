package com.mgd.core.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder
import software.amazon.awssdk.utils.builder.SdkBuilder

import java.util.regex.Pattern

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
    protected S3Client getS3Client() {

        S3ClientBuilder builder = S3Client.builder()

        if (parsedEndpoint) {
            builder.endpointOverride(parsedEndpoint)
                    .region(parsedRegion)
        }
        else if (parsedRegion) {
            builder.region(parsedRegion)
        }

        return builder
                .credentialsProvider(credentialsChain)
                .build()

    }

    @Internal
    protected S3AsyncClient getAsyncS3Client() {

        S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder()

        if (parsedEndpoint) {
            builder.endpointOverride(parsedEndpoint)
                    .region(parsedRegion)
        }
        else if (parsedRegion) {
            builder.region(parsedRegion)
        }

        return builder
                .credentialsProvider(credentialsChain)
                .build()

    }

    @Internal
    protected AwsCredentialsProviderChain getCredentialsChain() {

        ProfileCredentialsProvider profileCreds
        if (taskProfile) {
            logger.quiet("Using AWS credentials profile: ${profile}")
            profileCreds = ProfileCredentialsProvider.create(profile)
        }
        else {
            profileCreds = ProfileCredentialsProvider.create()
        }

        List<AwsCredentialsProvider> credentialsProviders = []
        credentialsProviders << EnvironmentVariableCredentialsProvider.create()
        credentialsProviders << SystemPropertyCredentialsProvider.create()
        credentialsProviders << profileCreds
        credentialsProviders << ContainerCredentialsProvider.builder().build()

        return AwsCredentialsProviderChain.builder()
                .credentialsProviders(credentialsProviders)
                .build()
    }

    @Internal
    protected URI getParsedEndpoint() {

        if (!taskEndpoint) {
            return null
        }

        if (taskRegion) {
             return URI.create(taskEndpoint)
        }
        else {
            throw new GradleException('Invalid parameters: [endpoint] is not valid without a provided [region]')
        }
    }

    @Internal
    protected Region getParsedRegion() {

        return taskRegion ? Region.of(taskRegion) : null
    }

    protected String parsePathPattern(String key) {

        if (key ==~ VALID_PATH_PATTERN) {
            String parsedKey = key.replaceAll(/(^\/)|(\*?\/$)|(\*$)/, '')
            if (!parsedKey.endsWith('/')) {
                return parsedKey
            }
        }
        throw new GradleException("Invalid pathPattern value: ${key}")
    }

    protected String parseKey(String key) {

        if ((key ==~ VALID_KEY_PATTERN) && !key.endsWith('/')) {
            return key
        }
        throw new GradleException("Invalid S3 path: ${key}")
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

    private Pattern VALID_PATH_PATTERN = ~/^([a-zA-Z0-9-_\.\/])+(\*)?(\*?\/)?$/
    private Pattern VALID_KEY_PATTERN = ~/^([a-zA-Z0-9-_\.\/])+$/
}
