package com.mgd.core.gradle

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
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

        s3Client = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(defaultEndpoint, defaultRegion))
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(localStack.accessKey, localStack.secretKey)
                )
            )
            .build()
    }
}
