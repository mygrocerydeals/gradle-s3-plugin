package com.mgd.core.gradle

import groovy.io.FileType
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import static org.assertj.core.api.Assertions.assertThat
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Spock test specification for Gradle S3 Download tasks configured for a provisioned LocalStack instance.
 */
@SuppressWarnings('LineLength')
class LocalStackS3DownloadTest extends LocalStackSpecification {

    def setupSpec() {

        seedS3DownloadBuckets()
        initializeTestProjectDirectory(DOWNLOAD_PROJECT_DIRECTORY)
    }

    def setup() {

        setupProjectDirectoryFiles()

        buildFile << """
            plugins {
                id 'com.mgd.core.gradle.s3'
            }
        """

        settingsFile << """
            rootProject.name='gradle-s3-plugin-download-test'
        """
    }

    def 'should download single S3 file'() {

        given:
        String filename = "${DOWNLOAD_PROJECT_DIRECTORY}/${SINGLE_DOWNLOAD_FILENAME}"
        buildFile << """

            task getSingleS3File(type: S3Download)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                endpoint = '${defaultEndpoint}'
                region = '${defaultRegion}'
                bucket = '${s3BucketName}'
                key = '${SINGLE_DOWNLOAD_FILENAME}'
                file = '${filename}'
            }
        """

        when:
        File file = new File(filename)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('getSingleS3File')
                .withPluginClasspath()
                .build()

        then:
        String s = "S3 Download s3://${s3BucketName}/${SINGLE_DOWNLOAD_FILENAME} -> ${filename}"
        assertThat(parseOutput(result.output)).contains(s)
        assertThat(result.task(':getSingleS3File').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
            .isFile()
            .hasName(SINGLE_DOWNLOAD_FILENAME)
    }

    def 'should download single S3 file with configuration cache enabled'() {

        given:
        String filename = "${DOWNLOAD_PROJECT_DIRECTORY}/${SINGLE_DOWNLOAD_FILENAME}"
        buildFile << """

            task getSingleS3FileCached(type: S3Download)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                endpoint = '${defaultEndpoint}'
                region = '${defaultRegion}'
                bucket = '${s3BucketName}'
                key = '${SINGLE_DOWNLOAD_FILENAME}'
                file = '${filename}'
            }
        """

        when:
        File file = new File(filename)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('--configuration-cache', 'getSingleS3FileCached')
                .withPluginClasspath()
                .build()

        then:
        String s = "S3 Download s3://${s3BucketName}/${SINGLE_DOWNLOAD_FILENAME} -> ${filename}"
        assertThat(parseOutput(result.output)).contains(s)
        assertThat(result.task(':getSingleS3FileCached').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isFile()
                .hasName(SINGLE_DOWNLOAD_FILENAME)
    }

    def 'should download S3 directory'() {

        given:
        buildFile << """

            task getS3Directory(type: S3Download)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                endpoint = '${defaultEndpoint}'
                region = '${defaultRegion}'
                bucket = '${s3BucketName}'
                destDir = '${DOWNLOAD_DIRECTORY_ROOT}'
                keyPrefix = '${SINGLE_DIRECTORY_NAME}'
                then = { File file ->
                    println "Downloaded file named \${file.name}"
                }
            }
        """

        when:
        String directoryPath = "${DOWNLOAD_PROJECT_DIRECTORY}/${DOWNLOAD_DIRECTORY_ROOT}"
        File file = new File(directoryPath)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('getS3Directory')
                .withPluginClasspath()
                .build()

        then:
        String s = "S3 Download recursive s3://${s3BucketName}/${SINGLE_DIRECTORY_NAME} -> ${DOWNLOAD_DIRECTORY_ROOT}"
        assertThat(parseOutput(result.output)).contains(s)
        assertThat(result.task(':getS3Directory').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES) { File f ->
            fileCount++
            assertThat(f.name).isEqualTo('directory-file.txt')
        }
        assertThat(fileCount).isEqualTo(1)
    }

    def 'should download S3 directory with configuration cache enabled'() {

        given:
        buildFile << """

            task getS3DirectoryCached(type: S3Download)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                endpoint = '${defaultEndpoint}'
                region = '${defaultRegion}'
                bucket = '${s3BucketName}'
                destDir = '${DOWNLOAD_DIRECTORY_ROOT}'
                keyPrefix = '${SINGLE_DIRECTORY_NAME}'
                then = { File file ->
                    println "Downloaded file named \${file.name}"
                }
            }
        """

        when:
        String directoryPath = "${DOWNLOAD_PROJECT_DIRECTORY}/${DOWNLOAD_DIRECTORY_ROOT}"
        File file = new File(directoryPath)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('--configuration-cache', 'getS3DirectoryCached')
                .withPluginClasspath()
                .build()

        then:
        String s = "S3 Download recursive s3://${s3BucketName}/${SINGLE_DIRECTORY_NAME} -> ${DOWNLOAD_DIRECTORY_ROOT}"
        assertThat(parseOutput(result.output)).contains(s)
        assertThat(result.task(':getS3DirectoryCached').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES) { File f ->
            fileCount++
            assertThat(f.name).isEqualTo('directory-file.txt')
        }
        assertThat(fileCount).isEqualTo(1)
    }

    def 'should download S3 path patterns'() {

        given:
        List<String> expectedFiles = [
            'single-file.txt', 'directory-file.txt', 'pattern-dir-2-file.txt',
            'non-matching-dir-1-file.txt', 'pattern-dir-1-file-1.txt', 'pattern-dir-1-file-2.txt'
        ]

        buildFile << """

            task getS3PathPatterns(type: S3Download)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                endpoint = '${defaultEndpoint}'
                region = '${defaultRegion}'
                bucket = '${s3BucketName}'
                destDir = '${DOWNLOAD_PATTERNS_ROOT}'
                pathPatterns = [
                    '${DIRECTORY_MATCHING_PATTERN}',
                    '${FILE_MATCHING_PATTERN}',
                    '${SINGLE_DIRECTORY_NAME}/',
                    '${SINGLE_DOWNLOAD_FILENAME}'
                ]
                then = { File file ->
                    println "Downloaded file named \${file.name}"
                }
            }
        """

        when:
        String directoryPath = "${DOWNLOAD_PROJECT_DIRECTORY}/${DOWNLOAD_PATTERNS_ROOT}"
        File file = new File(directoryPath)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('getS3PathPatterns')
                .withPluginClasspath()
                .build()

        then:
        String s = "S3 Download path patterns s3://${s3BucketName}/${DIRECTORY_MATCHING_PATTERN},${FILE_MATCHING_PATTERN},${SINGLE_DIRECTORY_NAME}/,${SINGLE_DOWNLOAD_FILENAME} -> ${DOWNLOAD_PATTERNS_ROOT}"
        assertThat(parseOutput(result.output)).contains(s)
        assertThat(result.task(':getS3PathPatterns').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
            .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES) { File f ->
            fileCount++
            assertThat(expectedFiles).contains(f.name)
        }
        assertThat(fileCount).isEqualTo(expectedFiles.size())
    }

    def 'should download S3 path patterns with configuration cache enabled'() {

        given:
        List<String> expectedFiles = [
                'single-file.txt', 'directory-file.txt', 'pattern-dir-2-file.txt',
                'non-matching-dir-1-file.txt', 'pattern-dir-1-file-1.txt', 'pattern-dir-1-file-2.txt'
        ]

        buildFile << """

            task getS3PathPatternsCached(type: S3Download)  {
                System.setProperty('aws.accessKeyId', '${accessKeyId}')
                System.setProperty('aws.secretKey', '${secretKey}')
                endpoint = '${defaultEndpoint}'
                region = '${defaultRegion}'
                bucket = '${s3BucketName}'
                destDir = '${DOWNLOAD_PATTERNS_ROOT}'
                pathPatterns = [
                    '${DIRECTORY_MATCHING_PATTERN}',
                    '${FILE_MATCHING_PATTERN}',
                    '${SINGLE_DIRECTORY_NAME}/',
                    '${SINGLE_DOWNLOAD_FILENAME}'
                ]
                then = { File file ->
                    println "Downloaded file named \${file.name}"
                }
            }
        """

        when:
        String directoryPath = "${DOWNLOAD_PROJECT_DIRECTORY}/${DOWNLOAD_PATTERNS_ROOT}"
        File file = new File(directoryPath)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('--configuration-cache', 'getS3PathPatternsCached')
                .withPluginClasspath()
                .build()

        then:
        String s = "S3 Download path patterns s3://${s3BucketName}/${DIRECTORY_MATCHING_PATTERN},${FILE_MATCHING_PATTERN},${SINGLE_DIRECTORY_NAME}/,${SINGLE_DOWNLOAD_FILENAME} -> ${DOWNLOAD_PATTERNS_ROOT}"
        assertThat(parseOutput(result.output)).contains(s)
        assertThat(result.task(':getS3PathPatternsCached').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES) { File f ->
            fileCount++
            assertThat(expectedFiles).contains(f.name)
        }
        assertThat(fileCount).isEqualTo(expectedFiles.size())
    }
}
