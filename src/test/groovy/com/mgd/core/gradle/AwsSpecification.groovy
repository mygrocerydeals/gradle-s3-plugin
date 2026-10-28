package com.mgd.core.gradle

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest
import software.amazon.awssdk.services.s3.model.VersioningConfiguration
import spock.lang.IgnoreIf

/**
 * Base Spock test specification for all tests which run against the concrete AWS EC2 cloud platform.
 * Skipped in CI when no AWS credentials are available (pull requests from forks do not receive repository secrets).
 */
@IgnoreIf(value = { env.CI && !env.AWS_ACCESS_KEY_ID }, inherited = true)
class AwsSpecification extends BaseSpecification {

    /**
     * Setup for the test run.
     */
    def setupSpec() {

        s3Client = S3Client.builder()
                        .region(Region.of(DEFAULT_REGION))
                        .build()

        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                                                .bucket(s3BucketName)
                                                .build()

        s3Client.createBucket(bucketRequest)

        // latency to give S3 time to propagate the new bucket
        sleep(700)

        // add versioning to the bucket
        PutBucketVersioningRequest versioningRequest = PutBucketVersioningRequest.builder()
                                                            .bucket(s3BucketName)
                                                            .versioningConfiguration(
                                                                VersioningConfiguration.builder()
                                                                    .status(BucketVersioningStatus.ENABLED)
                                                                    .build())
                                                            .build()

        s3Client.putBucketVersioning(versioningRequest)
    }

    /**
     * Tear down the S3 bucket used in the test run.
     */
    def cleanupSpec() {

        deleteS3Bucket()
    }
}
