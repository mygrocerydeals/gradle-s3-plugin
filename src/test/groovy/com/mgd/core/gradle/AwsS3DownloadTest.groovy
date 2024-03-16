package com.mgd.core.gradle

import groovy.io.FileType
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.assertj.core.api.Assertions.assertThat
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Spock test specification for Gradle S3 Download tasks configured for the AWS EC2 cloud.
 */
@SuppressWarnings('LineLength')
class AwsS3DownloadTest extends AwsSpecification {

    private static final Logger log = LoggerFactory.getLogger(AwsS3DownloadTest)

    def setupSpec() {

        seedS3DownloadBuckets(true)
        initializeTestProjectDirectory(DOWNLOAD_PROJECT_DIRECTORY)
        initializeTestKitDirectory(TEST_KIT_ROOT_DIRECTORY)
    }

    def setup() {

        setupProjectDirectoryFiles()

        buildFile << """
            plugins {
                id 'com.mgd.core.gradle.s3'
            }

            s3 {
                region = '${DEFAULT_REGION}'
                bucket = '${s3BucketName}'
            }
        """

        settingsFile << """
            rootProject.name='gradle-s3-plugin-download-test'
        """
    }

    def 'should download single S3 file'() {

        given:
        String filename = "${DOWNLOAD_DIRECTORY_PREFIX}/${SINGLE_DOWNLOAD_FILENAME}"
        File file = new File("${testKitParentDirectoryName}/${filename}")
        file.parentFile.mkdirs()
        buildFile << """

            task getSingleS3File(type: S3Download)  {
                key = '${SINGLE_DOWNLOAD_FILENAME}'
                file = '${filename}'
            }
        """

        when:
        BuildResult result
        try {
            result = GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments('getSingleS3File')
                    .withPluginClasspath()
                    .build()
        }
        catch (Exception e) {
            log.error("Exception thrown in download test: ${e.message}", e)
        }

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
        String filename = "${DOWNLOAD_DIRECTORY_PREFIX}/${SINGLE_DOWNLOAD_FILENAME}"
        File file = new File("${testKitParentDirectoryName}/${filename}")
        file.parentFile.mkdirs()
        buildFile << """

            task getSingleS3FileCached(type: S3Download)  {
                key = '${SINGLE_DOWNLOAD_FILENAME}'
                file = '${filename}'
            }
        """

        when:
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
        List<String> expectedFiles = [
                'directory-file.txt', 'directory-subfolder-file.txt'
        ]

        buildFile << """

            task getS3Directory(type: S3Download)  {
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
            assertThat(expectedFiles).contains(f.name)
        }
        assertThat(fileCount).isEqualTo(expectedFiles.size())
    }

    def 'should download nested S3 directory'() {

        given:
        buildFile << """

            task getS3Directory(type: S3Download)  {
                destDir = '${DOWNLOAD_DIRECTORY_ROOT}'
                keyPrefix = '${SINGLE_NESTED_DIRECTORY_NAME}'
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
        String s = "S3 Download recursive s3://${s3BucketName}/${SINGLE_NESTED_DIRECTORY_NAME} -> ${DOWNLOAD_DIRECTORY_ROOT}"
        assertThat(parseOutput(result.output)).contains(s)
        assertThat(result.task(':getS3Directory').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES) { File f ->
            fileCount++
            assertThat(f.name).isEqualTo('directory-subfolder-file.txt')
        }
        assertThat(fileCount).isEqualTo(1)
    }

    def 'should download S3 directory with configuration cache enabled'() {

        given:
        List<String> expectedFiles = [
                'directory-file.txt', 'directory-subfolder-file.txt'
        ]

        buildFile << """

            task getS3DirectoryCached(type: S3Download)  {
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
            assertThat(expectedFiles).contains(f.name)
        }
        assertThat(fileCount).isEqualTo(expectedFiles.size())
    }

    def 'should download S3 path patterns'() {

        given:
        List<String> expectedFiles = [
            'single-file.txt', 'directory-file.txt', 'pattern-dir-2-file.txt',
            'pattern-dir-1-file-1.txt', 'pattern-dir-1-file-2.txt', 'directory-subfolder-file.txt',
            'multi-match-dir-1-file-1.txt', 'multi-match-dir-2-file-1.txt'
        ]

        buildFile << """

            task getS3PathPatterns(type: S3Download)  {
                destDir = '${DOWNLOAD_PATTERNS_ROOT}'
                pathPatterns = [
                    '${SIMPLE_FILE_MATCHING_PATTERN}',
                    '${MULTI_DIRECTORY_MATCHING_PATTERN}',
                    '${COMPOUND_FILE_MATCHING_PATTERN}',
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
        String s = "S3 Download path patterns s3://${s3BucketName}/${SIMPLE_FILE_MATCHING_PATTERN},${MULTI_DIRECTORY_MATCHING_PATTERN},${COMPOUND_FILE_MATCHING_PATTERN},${SINGLE_DIRECTORY_NAME}/,${SINGLE_DOWNLOAD_FILENAME} -> ${DOWNLOAD_PATTERNS_ROOT}"
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

    def 'should download S3 path patterns for directories'() {

        given:
        List<String> expectedFiles = [
            'directory-file.txt', 'directory-subfolder-file.txt'
        ]

        buildFile << """

            task getS3PathPatterns(type: S3Download)  {
                destDir = '${DOWNLOAD_PATTERNS_ROOT}'
                pathPatterns = [
                    '${DIRECTORY_MATCHING_PATTERN}/'
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
        String s = "S3 Download path patterns s3://${s3BucketName}/${DIRECTORY_MATCHING_PATTERN}/ -> ${DOWNLOAD_PATTERNS_ROOT}"
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
            'pattern-dir-1-file-1.txt', 'pattern-dir-1-file-2.txt', 'directory-subfolder-file.txt'
        ]

        buildFile << """

            task getS3PathPatternsCached(type: S3Download)  {
                destDir = '${DOWNLOAD_PATTERNS_ROOT}'
                pathPatterns = [
                    '${SIMPLE_FILE_MATCHING_PATTERN}',
                    '${COMPOUND_FILE_MATCHING_PATTERN}',
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
        String s = "S3 Download path patterns s3://${s3BucketName}/${SIMPLE_FILE_MATCHING_PATTERN},${COMPOUND_FILE_MATCHING_PATTERN},${SINGLE_DIRECTORY_NAME}/,${SINGLE_DOWNLOAD_FILENAME} -> ${DOWNLOAD_PATTERNS_ROOT}"
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
