package com.mgd.core.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

import static org.assertj.core.api.Assertions.assertThat
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Spock test specification for Gradle S3 Upload tasks configured for the AWS EC2 cloud.
 */
class AwsS3UploadTest extends AwsSpecification {

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
                region = '${DEFAULT_REGION}'
                bucket = '${s3BucketName}'
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
        assertThat(messages).contains(fileUploadMessage(s3BucketName, SINGLE_UPLOAD_FILENAME, filename))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
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
                region = '${DEFAULT_REGION}'
                bucket = '${s3BucketName}'
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
        assertThat(messages).contains(fileUploadMessage(s3BucketName, SINGLE_UPLOAD_FILENAME, filename))
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
                region = '${DEFAULT_REGION}'
                bucket = '${s3BucketName}'
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
        assertThat(messages).contains(directoryUploadMessage(s3BucketName, UPLOAD_DIRECTORY_NAME, UPLOAD_DIRECTORY_NAME))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putS3Directory').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()
        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).contains(*expectedKeys)
    }

    def 'should upload directory to S3 with configuration cache enabled'() {

        given:
        List<String> expectedKeys = seedDirectoryUploadFiles().collect { String filename ->
            "${UPLOAD_DIRECTORY_NAME}/${filename}".toString()
        }
        buildFile << """

            task putS3DirectoryCached(type: S3Upload)  {
                region = '${DEFAULT_REGION}'
                bucket = '${s3BucketName}'
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
        assertThat(messages).contains(directoryUploadMessage(s3BucketName, UPLOAD_DIRECTORY_NAME, UPLOAD_DIRECTORY_NAME))
        assertThat(messages).doesNotContain(PATH_STYLE_MESSAGE)
        assertThat(result.task(':putS3DirectoryCached').outcome).isEqualTo(SUCCESS)

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()
        List<String> keys = s3Client.listObjectsV2(request).contents()*.key()
        assertThat(keys).contains(*expectedKeys)
    }
}
