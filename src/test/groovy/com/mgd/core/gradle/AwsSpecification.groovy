package com.mgd.core.gradle

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import spock.lang.Specification

import java.text.SimpleDateFormat

class AwsSpecification extends Specification {

    static final String DEFAULT_REGION = 'us-east-1'

    static AmazonS3 s3Client
    static String s3BucketName

    def setupSpec() {

        SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd-HHmmss')
        s3BucketName = "gradle-s3-plugin-test-${df.format(new Date())}"

        s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(DEFAULT_REGION)
            .build()
        s3Client.createBucket(s3BucketName)

        // latency to give S3 time to propagate the new bucket
        sleep(500)
    }

    def cleanupSpec() {

        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        if (keys) {
            s3Client.deleteObjects(new DeleteObjectsRequest(s3BucketName)
                .withKeys(keys.collect { new DeleteObjectsRequest.KeyVersion(it) }))
        }
        s3Client.deleteBucket(s3BucketName)
    }
}
