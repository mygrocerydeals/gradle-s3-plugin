package com.mgd.core.gradle

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import groovy.io.FileType
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.assertj.core.api.Assertions.assertThat
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3

@Testcontainers
class S3UploadTest extends Specification {

    static final String BUILD_FILE = 'build.gradle'
    static final String SETTINGS_FILE = 'settings.gradle'
    static final String PROJECT_DIRECTORY = 'build/tmp/test/S3UploadTest'
    static final String RESOURCES_DIRECTORY = 'src/test/resources/s3-upload-files'

    static final String SINGLE_UPLOAD_FILENAME = 'single-file-upload.txt'
    static final String UPLOAD_DIRECTORY_NAME = 'directory-upload'
    
    static File testProjectDir
    static String defaultEndpoint
    static String defaultRegion
    static String s3BucketName
    static String accessKeyId
    static String secretKey
    static AmazonS3 s3Client

    File buildFile
    File settingsFile

    @Shared
    LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0.2")
    )

    def setupSpec() {
        SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd-HHmmss')
        s3BucketName = "gradle-s3-plugin-upload-test-${df.format(new Date())}"

        localStack.execInContainer("awslocal", "s3", "mb", "s3://" + s3BucketName)

        defaultEndpoint = localStack.getEndpointOverride(S3).toString()
        defaultRegion = localStack.getRegion()
        accessKeyId = localStack.getAccessKey()
        secretKey = localStack.getSecretKey()

        s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(defaultEndpoint, defaultRegion))
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(localStack.getAccessKey(), localStack.getSecretKey())
                        )
                ).build()

        testProjectDir = new File(PROJECT_DIRECTORY)
        if (testProjectDir.exists()) {
            testProjectDir.deleteDir()
        }
    }

    def setup() {
        testProjectDir.mkdirs()
        buildFile = new File(testProjectDir, BUILD_FILE)
        settingsFile = new File(testProjectDir, SETTINGS_FILE)

        // start with an empty s3 bucket at the start of each test
        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        if (keys) {
           s3Client.deleteObjects(new DeleteObjectsRequest(s3BucketName)
                .withKeys(keys.collect { new DeleteObjectsRequest.KeyVersion(it) }))
        }

        // latency to give S3 time to propagate the command
        sleep(500)

        [buildFile, settingsFile].each { File file ->
            if (file.exists()) {
                file.delete()
            }
        }

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
        String filename = "${RESOURCES_DIRECTORY}/${SINGLE_UPLOAD_FILENAME}"
        buildFile << """

            task putSingleS3File(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
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
        String filename = "${RESOURCES_DIRECTORY}/${SINGLE_UPLOAD_FILENAME}"
        buildFile << """

            task putSingleS3FileCached(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
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
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
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
        assertThat(keys).containsAll(expectedKeys)
    }

    def 'should upload directory to S3 with configuration cache enabled'() {

        given:
        List<String> expectedKeys = seedUploadFiles().collect {String filename ->
            "${UPLOAD_DIRECTORY_NAME}/${filename}".toString()
        }
        buildFile << """

            task putS3DirectoryCached(type: S3Upload)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
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
        assertThat(keys).containsAll(expectedKeys)
    }

    /**
     * Helper method to seed the "fake" Gradle test project root with files from the resources directory of the "real"
     * Gradle S3 Plugin project.
     */
    private List<String> seedUploadFiles() {

        List<String> filenames = []

        String resourcesDirectoryName = "${RESOURCES_DIRECTORY}/${UPLOAD_DIRECTORY_NAME}"
        String projectDirectoryName = "${PROJECT_DIRECTORY}/${UPLOAD_DIRECTORY_NAME}"

        File resourcesDir = new File(resourcesDirectoryName)
        File projectDir = new File(projectDirectoryName)
        projectDir.mkdirs()
        resourcesDir.eachFile(FileType.FILES, {File file ->
            File target = new File(projectDirectoryName, file.name)
            target << file.text
            filenames << file.name
        })

        return filenames
    }
}
