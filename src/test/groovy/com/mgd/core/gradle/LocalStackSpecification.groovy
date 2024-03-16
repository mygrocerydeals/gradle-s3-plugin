package com.mgd.core.gradle

import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import spock.lang.Shared

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3

/**
 * Base Spock test specification for all tests which run against a provisioned LocalStack container.
 */
@Testcontainers
@SuppressWarnings('AssignmentToStaticFieldFromInstanceMethod')
class LocalStackSpecification extends BaseSpecification {

    @Shared
    private final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse('localstack/localstack:3.0.2')
    )

    protected static String defaultEndpoint
    protected static String defaultRegion
    protected static String accessKeyId
    protected static String secretKey

    /**
     * Setup for the test run.
     */
    def setupSpec() {

        localStack.execInContainer('awslocal', 's3', 'mb', "s3://$s3BucketName")

        defaultEndpoint = localStack.getEndpointOverride(S3).toString()
        defaultRegion = localStack.region
        accessKeyId = localStack.accessKey
        secretKey = localStack.secretKey

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.accessKey, localStack.secretKey))

        s3Client = S3Client.builder()
                        .credentialsProvider(credentialsProvider)
                        .endpointOverride(URI.create(defaultEndpoint))
                        .region(Region.of(defaultRegion))
                        .build()
    }
}
