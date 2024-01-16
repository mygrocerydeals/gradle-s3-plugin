package com.mgd.core.gradle

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import groovy.io.FileType
import spock.lang.Specification

import java.text.SimpleDateFormat

/**
 * Shared base Spock test specification for all tests to extend from. Includes common properties and methods for
 * test setup and teardown.
 */
class BaseSpecification extends Specification {

    static final String BUILD_FILE = 'build.gradle'
    static final String SETTINGS_FILE = 'settings.gradle'

    static final String DOWNLOAD_PROJECT_DIRECTORY = 'build/tmp/test/S3DownloadTest'
    static final String DOWNLOAD_RESOURCES_DIRECTORY = 'src/test/resources/s3-download-bucket'
    static final String UPLOAD_PROJECT_DIRECTORY = 'build/tmp/test/S3UploadTest'
    static final String UPLOAD_RESOURCES_DIRECTORY = 'src/test/resources/s3-upload-files'

    static final String DOWNLOAD_DIRECTORY_ROOT = 'download-dir-test'
    static final String DOWNLOAD_PATTERNS_ROOT = 'download-patterns-test'
    static final String SINGLE_DOWNLOAD_FILENAME = 'single-file.txt'

    static final String SINGLE_UPLOAD_FILENAME = 'single-file-upload.txt'
    static final String UPLOAD_DIRECTORY_NAME = 'directory-upload'

    static final String SINGLE_DIRECTORY_NAME = 'single-directory'
    static final String DIRECTORY_MATCHING_PATTERN = 'pattern-dir-1*'
    static final String FILE_MATCHING_PATTERN = 'pattern-dir-2/pattern*'

    static final String DEFAULT_REGION = 'us-east-1'

    static AmazonS3 s3Client
    static String s3BucketName

    static File testProjectDir

    File singleDownloadFile
    File buildFile
    File settingsFile

    /**
     * Getter to generate a unique S3 bucket name for the test.
     */
    protected static String getS3BucketName() {

        if (!s3BucketName) {
            SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd-HHmmssSSS', Locale.US)
            s3BucketName = "gradle-s3-plugin-test-${df.format(new Date())}"
        }

        return s3BucketName
    }

    /**
     * Helper method to initialize the "fake" Gradle test project root directory.
     */
    protected static void initializeTestProjectDirectory(String name) {

        testProjectDir = new File(name)

        if (testProjectDir.exists()) {
            testProjectDir.deleteDir()
        }
    }

    /**
     * Helper method to seed the "fake" Gradle test project root with files from the resources directory of the "real"
     * Gradle S3 Plugin project.
     */
    protected static List<String> seedUploadFiles() {

        List<String> filenames = []

        String resourcesDirectoryName = "${UPLOAD_RESOURCES_DIRECTORY}/${UPLOAD_DIRECTORY_NAME}"
        String projectDirectoryName = "${UPLOAD_PROJECT_DIRECTORY}/${UPLOAD_DIRECTORY_NAME}"

        File resourcesDir = new File(resourcesDirectoryName)
        File projectDir = new File(projectDirectoryName)
        projectDir.mkdirs()
        resourcesDir.eachFile(FileType.FILES) { File file ->
            File target = new File(projectDirectoryName, file.name)
            target << file.text
            filenames << file.name
        }

        return filenames
    }

    /**
     * Helper method to seed the S3 bucket with files for the download tests.
     */
    protected static void seedS3DownloadBuckets(boolean addLatency = false) {

        // unfortunately, we have to deal with platform-dependent path separators
        String parentRoot = DOWNLOAD_RESOURCES_DIRECTORY.split(/\//).join(File.separator)

        File resourceDir = new File(DOWNLOAD_RESOURCES_DIRECTORY)
        resourceDir.eachFileRecurse(FileType.FILES) { File file ->
            String prefix = file.parent.replace(parentRoot, '').replace(File.separator, '')
            String key = prefix ? "${prefix}/${file.name}" : file.name
            s3Client.putObject(s3BucketName, key, file)
        }

        if (addLatency) {
            // latency to allow for the file content to be fully written to storage
            sleep(1000)
        }
    }

    /**
     * Helper method to remove all object keys and rest the test S3 bucket to a pristine state.
     */
    protected static void clearS3Bucket() {

        List<String> keys = s3Client.listObjects(s3BucketName).objectSummaries*.key
        if (keys) {
            s3Client.deleteObjects(new DeleteObjectsRequest(s3BucketName)
                    .withKeys(keys.collect { new DeleteObjectsRequest.KeyVersion(it) }))
        }
    }

    /**
     * Helper method to delete the test S3 bucket.
     */
    protected static void deleteS3Bucket() {

        clearS3Bucket()
        s3Client.deleteBucket(s3BucketName)
    }

    /**
     * Helper method to reset the test project directory state and initialize the file handles used in the test run.
     */
    protected void setupProjectDirectoryFiles() {

        testProjectDir.mkdirs()

        buildFile = new File(testProjectDir, BUILD_FILE)
        settingsFile = new File(testProjectDir, SETTINGS_FILE)
        singleDownloadFile = new File(SINGLE_DOWNLOAD_FILENAME)

        [buildFile, settingsFile, singleDownloadFile].each { File file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /**
     * Helper method to parse an output buffer to an array of normalized strings
     */
    protected String[] parseOutput(String output) {

        return output
                .split(/\n/)
                *.trim()
    }
}
