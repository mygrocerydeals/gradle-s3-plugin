package com.mgd.core.gradle


import com.amazonaws.services.s3.AmazonS3ClientBuilder

/**
 * Base Spock test specification for all tests which run against the concrete AWS EC2 cloud platform.
 */
class AwsSpecification extends BaseSpecification {

    /**
     * Setup for the test run.
     */
    def setupSpec() {

        s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(DEFAULT_REGION)
            .build()
        s3Client.createBucket(s3BucketName)

        // latency to give S3 time to propagate the new bucket
        sleep(500)
    }

    /**
     * Tear down the S3 bucket used in the test run.
     */
    def cleanupSpec() {

        deleteS3Bucket()
    }
}
