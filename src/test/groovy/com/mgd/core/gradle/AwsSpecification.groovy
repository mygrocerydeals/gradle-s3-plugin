package com.mgd.core.gradle

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest

/**
 * Base Spock test specification for all tests which run against the concrete AWS EC2 cloud platform.
 */
class AwsSpecification extends BaseSpecification {

    /**
     * Setup for the test run.
     */
    def setupSpec() {

        s3Client = S3Client.builder()
                        .region(Region.of(DEFAULT_REGION))
                        .build()

        CreateBucketRequest request = CreateBucketRequest.builder()
                                            .bucket(s3BucketName)
                                            .build()

        s3Client.createBucket(request)

        // latency to give S3 time to propagate the new bucket
        sleep(700)
    }

    /**
     * Tear down the S3 bucket used in the test run.
     */
    def cleanupSpec() {

        deleteS3Bucket()
    }
}
