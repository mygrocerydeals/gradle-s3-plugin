package com.mgd.core.gradle

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import groovy.io.FileType
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.assertj.core.api.Assertions.assertThat
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class S3DownloadTest extends Specification {

    static final String BUILD_FILE = 'build.gradle'
    static final String SETTINGS_FILE = 'settings.gradle'
    static final String PROJECT_DIRECTORY = 'build/tmp/test/S3DownloadTest'
    static final String RESOURCES_DIRECTORY = 'src/test/resources/s3-download-bucket'

    static final String DEFAULT_REGION = 'us-east-1'

    static final String DOWNLOAD_DIRECTORY_ROOT = 'download-dir-test'
    static final String DOWNLOAD_PATTERNS_ROOT = 'download-patterns-test'
    static final String SINGLE_DOWNLOAD_FILENAME = 'single-file.txt'
    static final String SINGLE_DIRECTORY_NAME = 'single-directory'
    static final String DIRECTORY_MATCHING_PATTERN = 'pattern-dir-1*'
    static final String FILE_MATCHING_PATTERN = 'pattern-dir-2/pattern*'

    static File testProjectDir
    static String s3BucketName
    static AmazonS3 s3Client

    File singleDownloadFile = new File(SINGLE_DOWNLOAD_FILENAME)
    File buildFile
    File settingsFile

    def setupSpec() {

        SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd-HHmmss')
        s3BucketName = "gradle-s3-plugin-download-test-${df.format(new Date())}"

        s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(DEFAULT_REGION)
                        .build()
        s3Client.createBucket(s3BucketName)

        // latency to give S3 time to propagate the new bucket
        sleep(500)

        // unfortunately, we have to deal with platform-dependent path separators
        String parentRoot = RESOURCES_DIRECTORY.split("\\/").join(File.separator)

        File resourceDir = new File(RESOURCES_DIRECTORY)
        resourceDir.eachFileRecurse(FileType.FILES, { File file ->
            String prefix = file.parent.replace(parentRoot, '').replace(File.separator, '')
            String key = prefix ? "${prefix}/${file.name}" : file.name
            s3Client.putObject(s3BucketName, key, file)
        })

        // latency to allow for the file content to be fully written to storage
        sleep(1000)

        testProjectDir = new File(PROJECT_DIRECTORY)
        if (testProjectDir.exists()) {
            testProjectDir.deleteDir()
        }
    }

    def cleanupSpec() {

        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        if (keys) {
            s3Client.deleteObjects(new DeleteObjectsRequest(s3BucketName)
                    .withKeys(keys.collect { new DeleteObjectsRequest.KeyVersion(it) }))
        }
        s3Client.deleteBucket(s3BucketName)
    }

    def setup() {

        testProjectDir.mkdirs()
        buildFile = new File(testProjectDir, BUILD_FILE)
        settingsFile = new File(testProjectDir, SETTINGS_FILE)

        [buildFile, settingsFile, singleDownloadFile].each { File file ->
            if (file.exists()) {
                file.delete()
            }
        }

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
        String filename = "${PROJECT_DIRECTORY}/${SINGLE_DOWNLOAD_FILENAME}"
        buildFile << """

            task getSingleS3File(type: S3Download)  {
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
        assertThat(result.output).contains("S3 Download s3://${s3BucketName}/${SINGLE_DOWNLOAD_FILENAME} -> ${filename}")
        assertThat(result.task(':getSingleS3File').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
            .isFile()
            .hasName(SINGLE_DOWNLOAD_FILENAME)
    }

    def 'should download single S3 file with configuration cache enabled'() {

        given:
        String filename = "${PROJECT_DIRECTORY}/${SINGLE_DOWNLOAD_FILENAME}"
        buildFile << """

            task getSingleS3FileCached(type: S3Download)  {
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
        assertThat(result.output).contains("S3 Download s3://${s3BucketName}/${SINGLE_DOWNLOAD_FILENAME} -> ${filename}")
        assertThat(result.task(':getSingleS3FileCached').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isFile()
                .hasName(SINGLE_DOWNLOAD_FILENAME)
    }

    def 'should download S3 directory'() {

        given:
        buildFile << """

            task getS3Directory(type: S3Download)  {
                bucket = '${s3BucketName}'
                destDir = '${DOWNLOAD_DIRECTORY_ROOT}'
                keyPrefix = '${SINGLE_DIRECTORY_NAME}'
                then = { File file ->
                    println "Downloaded file named \${file.name}"
                }
            }
        """

        when:
        String directoryPath = "${PROJECT_DIRECTORY}/${DOWNLOAD_DIRECTORY_ROOT}"
        File file = new File(directoryPath)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('getS3Directory')
                .withPluginClasspath()
                .build()

        then:
        assertThat(result.output).contains("S3 Download recursive s3://${s3BucketName}/${SINGLE_DIRECTORY_NAME} -> ${DOWNLOAD_DIRECTORY_ROOT}")
        assertThat(result.task(':getS3Directory').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES, { File f ->
            fileCount++
            assertThat(f.name).isEqualTo('directory-file.txt')
        })
        assertThat(fileCount).isEqualTo(1)
    }

    def 'should download S3 directory with configuration cache enabled'() {

        given:
        buildFile << """

            task getS3DirectoryCached(type: S3Download)  {
                bucket = '${s3BucketName}'
                destDir = '${DOWNLOAD_DIRECTORY_ROOT}'
                keyPrefix = '${SINGLE_DIRECTORY_NAME}'
                then = { File file ->
                    println "Downloaded file named \${file.name}"
                }
            }
        """

        when:
        String directoryPath = "${PROJECT_DIRECTORY}/${DOWNLOAD_DIRECTORY_ROOT}"
        File file = new File(directoryPath)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('--configuration-cache', 'getS3DirectoryCached')
                .withPluginClasspath()
                .build()

        then:
        assertThat(result.output).contains("S3 Download recursive s3://${s3BucketName}/${SINGLE_DIRECTORY_NAME} -> ${DOWNLOAD_DIRECTORY_ROOT}")
        assertThat(result.task(':getS3DirectoryCached').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES, { File f ->
            fileCount++
            assertThat(f.name).isEqualTo('directory-file.txt')
        })
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
        String directoryPath = "${PROJECT_DIRECTORY}/${DOWNLOAD_PATTERNS_ROOT}"
        File file = new File(directoryPath)
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments( 'getS3PathPatterns')
                .withPluginClasspath()
                .build()

        then:
        assertThat(result.output).contains("S3 Download path patterns s3://${s3BucketName}/${DIRECTORY_MATCHING_PATTERN},${FILE_MATCHING_PATTERN},${SINGLE_DIRECTORY_NAME}/,${SINGLE_DOWNLOAD_FILENAME} -> ${DOWNLOAD_PATTERNS_ROOT}")
        assertThat(result.task(':getS3PathPatterns').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
            .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES, { File f ->
            fileCount++
            assertThat(expectedFiles).contains(f.name)
        })
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
        String directoryPath = "${PROJECT_DIRECTORY}/${DOWNLOAD_PATTERNS_ROOT}"
        File file = new File(directoryPath)
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('--configuration-cache', 'getS3PathPatternsCached')
                .withPluginClasspath()
                .build()

        then:
        assertThat(result.output).contains("S3 Download path patterns s3://${s3BucketName}/${DIRECTORY_MATCHING_PATTERN},${FILE_MATCHING_PATTERN},${SINGLE_DIRECTORY_NAME}/,${SINGLE_DOWNLOAD_FILENAME} -> ${DOWNLOAD_PATTERNS_ROOT}")
        assertThat(result.task(':getS3PathPatternsCached').outcome).isEqualTo(SUCCESS)
        assertThat(file).exists()
                .isDirectory()

        int fileCount = 0
        file.eachFileRecurse(FileType.FILES, { File f ->
            fileCount++
            assertThat(expectedFiles).contains(f.name)
        })
        assertThat(fileCount).isEqualTo(expectedFiles.size())
    }
}
