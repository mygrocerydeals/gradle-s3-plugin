package com.mgd.core.gradle

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3

@Testcontainers
class LocalStackSpecification extends Specification {

    @Shared
    private LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0.2")
    )

    static AmazonS3 s3Client

    static String defaultEndpoint
    static String defaultRegion
    static String s3BucketName
    static String accessKeyId
    static String secretKey

    def setupSpec() {

        SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd-HHmmss')
        s3BucketName = "gradle-s3-plugin-test-${df.format(new Date())}"

        localStack.execInContainer('awslocal', 's3', 'mb', "s3://$s3BucketName")

        defaultEndpoint = localStack.getEndpointOverride(S3).toString()
        defaultRegion = localStack.getRegion()
        accessKeyId = localStack.getAccessKey()
        secretKey = localStack.getSecretKey()

        s3Client = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(defaultEndpoint, defaultRegion))
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(localStack.getAccessKey(), localStack.getSecretKey())
                )
            )
            .build()
    }

}
