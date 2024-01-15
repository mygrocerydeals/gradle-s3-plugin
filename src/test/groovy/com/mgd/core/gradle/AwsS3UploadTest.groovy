package com.mgd.core.gradle


import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

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
        String filename = "${UPLOAD_RESOURCES_DIRECTORY}/${SINGLE_UPLOAD_FILENAME}"
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
        assertThat(result.output).contains("S3 Upload ${filename} -> s3://${s3BucketName}/${SINGLE_UPLOAD_FILENAME}")
        assertThat(result.task(':putSingleS3File').outcome).isEqualTo(SUCCESS)

        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        assertThat(keys).isEqualTo(['single-file-upload.txt'])
    }

    def 'should upload single file to S3 with configuration cache enabled'() {

        given:
        String filename = "${UPLOAD_RESOURCES_DIRECTORY}/${SINGLE_UPLOAD_FILENAME}"
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
        assertThat(result.output).contains("S3 Upload ${filename} -> s3://${s3BucketName}/${SINGLE_UPLOAD_FILENAME}")
        assertThat(result.task(':putSingleS3FileCached').outcome).isEqualTo(SUCCESS)

        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        assertThat(keys).isEqualTo(['single-file-upload.txt'])
    }

    def 'should upload directory to S3'() {

        given:
        List<String> expectedKeys = seedUploadFiles().collect {String filename ->
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
        assertThat(result.output).contains("S3 Upload directory ${UPLOAD_DIRECTORY_NAME} -> s3://${s3BucketName}/${UPLOAD_DIRECTORY_NAME}")
        assertThat(result.task(':putS3Directory').outcome).isEqualTo(SUCCESS)

        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        assertThat(keys).contains(*expectedKeys)
    }

    def 'should upload directory to S3 with configuration cache enabled'() {

        given:
        List<String> expectedKeys = seedUploadFiles().collect {String filename ->
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
        assertThat(result.output).contains("S3 Upload directory ${UPLOAD_DIRECTORY_NAME} -> s3://${s3BucketName}/${UPLOAD_DIRECTORY_NAME}")
        assertThat(result.task(':putS3DirectoryCached').outcome).isEqualTo(SUCCESS)

        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        assertThat(keys).contains(*expectedKeys)
    }
}
