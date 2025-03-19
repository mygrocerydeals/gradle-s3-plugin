package com.mgd.core.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

import static org.assertj.core.api.Assertions.assertThat
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Spock test specification for Gradle S3 Download tasks configured for a provisioned LocalStack instance.
 */
class LocalStackS3UploadTest extends LocalStackSpecification {

    def setupSpec() {

        initializeTestProjectDirectory(UPLOAD_PROJECT_DIRECTORY)
    }

    def setup() {

        // start with an empty s3 bucket at the start of each test
        clearS3Bucket()
        setupProjectDirectoryFiles()

        buildFile << """
            plugins {
                id 'com.mgd.core.gradle.s3'
            }

            s3 {
                endpoint = '${defaultEndpoint}'
                region = '${defaultRegion}'
                bucket = '${s3BucketName}'
            }
        """

        settingsFile << """
            rootProject.name='gradle-s3-plugin-upload-test'
        """
    }

    def 'should upload single file to S3'() {

        given:
        String filename = seedSingleUploadFile()
        buildFile << """

            task putSingleS3File(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                key = '${SINGLE_UPLOAD_FILENAME}'
                file = '${filename}'
            }
        """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('putSingleS3File')
                .withPluginClasspath()
                .build()

        then:
        List<String> messages = parseOutput(result.output)
        assertThat(messages).contains(fileUploadMessage(s3BucketName, SINGLE_UPLOAD_FILENAME, filename, false))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putSingleS3File').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()
        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).isEqualTo(['single-file-upload.txt'])


        //validate default content type
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(s3BucketName)
                .key(keys.first)
                .build()

        HeadObjectResponse response = s3Client.headObject(headRequest)
        //https://github.com/assertj/doc/issues/167#issuecomment-2491657623
        !! assertThat(response.contentType()).isEqualTo('application/octet-stream')
    }

    def 'should upload single file to S3 with content-type'() {

        given:
        String filename = seedSingleUploadFile()
        buildFile << """

            task putSingleS3File(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                key = '${SINGLE_UPLOAD_FILENAME}'
                file = '${filename}'
                contentType = 'text/plain'
            }
        """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('putSingleS3File')
                .withPluginClasspath()
                .build()

        then:
        List<String> messages = parseOutput(result.output)
        assertThat(messages).contains(fileUploadMessage(s3BucketName, SINGLE_UPLOAD_FILENAME, filename, false))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putSingleS3File').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()

        var content = s3Client.listObjectsV2(request).contents()

        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).isEqualTo(['single-file-upload.txt'])

        //validate provided content type
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(s3BucketName)
                .key(keys.first)
                .build()

        HeadObjectResponse response = s3Client.headObject(headRequest)
        //https://github.com/assertj/doc/issues/167#issuecomment-2491657623
        !! assertThat(response.contentType()).isEqualTo('text/plain')
    }

    def 'should upload single file to S3 with path-style url'() {

        given:
        String filename = seedSingleUploadFile()
        buildFile << """

            task putSingleS3File(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                usePathStyleUrl = true
                key = '${SINGLE_UPLOAD_FILENAME}'
                file = '${filename}'
            }
        """

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('putSingleS3File')
            .withPluginClasspath()
            .build()

        then:
        List<String> messages = parseOutput(result.output)
        assertThat(messages).contains(fileUploadMessage(s3BucketName, SINGLE_UPLOAD_FILENAME, filename, false))
        assertThat(messages).contains(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putSingleS3File').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(s3BucketName)
            .build()
        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).isEqualTo(['single-file-upload.txt'])
    }

    def 'should upload single file to S3 with configuration cache enabled'() {

        given:
        String filename = seedSingleUploadFile()
        buildFile << """

            task putSingleS3FileCached(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                key = '${SINGLE_UPLOAD_FILENAME}'
                file = '${filename}'
            }
        """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('--configuration-cache', 'putSingleS3FileCached')
                .withPluginClasspath()
                .build()

        then:
        List<String> messages = parseOutput(result.output)
        assertThat(messages).contains(fileUploadMessage(s3BucketName, SINGLE_UPLOAD_FILENAME, filename, false))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putSingleS3FileCached').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()
        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).isEqualTo(['single-file-upload.txt'])
    }

    def 'should upload directory to S3'() {

        given:
        List<String> expectedKeys = seedDirectoryUploadFiles().collect { String filename ->
            "${UPLOAD_DIRECTORY_NAME}/${filename}".toString()
        }
        buildFile << """

            task putS3Directory(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                sourceDir = '${UPLOAD_DIRECTORY_NAME}'
                keyPrefix = '${UPLOAD_DIRECTORY_NAME}'
            }
        """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('putS3Directory')
                .withPluginClasspath()
                .build()

        then:
        List<String> messages = parseOutput(result.output)
        assertThat(messages).contains(directoryUploadMessage(s3BucketName, UPLOAD_DIRECTORY_NAME, UPLOAD_DIRECTORY_NAME, false))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putS3Directory').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()
        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).containsAll(expectedKeys)
    }

    def 'should upload directory to S3 with configuration cache enabled'() {

        given:
        List<String> expectedKeys = seedDirectoryUploadFiles().collect { String filename ->
            "${UPLOAD_DIRECTORY_NAME}/${filename}".toString()
        }
        buildFile << """

            task putS3DirectoryCached(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                sourceDir = '${UPLOAD_DIRECTORY_NAME}'
                keyPrefix = '${UPLOAD_DIRECTORY_NAME}'
            }
        """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('--configuration-cache', 'putS3DirectoryCached')
                .withPluginClasspath()
                .build()

        then:
        List<String> messages = parseOutput(result.output)
        assertThat(messages).contains(directoryUploadMessage(s3BucketName, UPLOAD_DIRECTORY_NAME, UPLOAD_DIRECTORY_NAME, false))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putS3DirectoryCached').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()
        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).containsAll(expectedKeys)
    }
}
