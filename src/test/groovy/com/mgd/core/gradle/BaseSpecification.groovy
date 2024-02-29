package com.mgd.core.gradle

import groovy.io.FileType
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import spock.lang.Specification

import java.text.SimpleDateFormat

/**
 * Shared base Spock test specification for all tests to extend from. Includes common properties and methods for
 * test setup and teardown.
 */
class BaseSpecification extends Specification {

    static final String GRADLE_VERSION = '8.6'
    static final String DAEMON_PREFIX = 'test-kit-daemon'

    static final String BUILD_FILE = 'build.gradle'
    static final String SETTINGS_FILE = 'settings.gradle'

    static final String DOWNLOAD_DIRECTORY_PREFIX = 'S3DownloadTest'
    static final String UPLOAD_DIRECTORY_PREFIX = 'S3UploadTest'
    static final String DOWNLOAD_PROJECT_DIRECTORY = 'build/tmp/test/S3DownloadTest'
    static final String LOCALSTACK_DOWNLOAD_PROJECT_DIRECTORY = 'build/tmp/test/S3LocalStackDownloadTest'
    static final String DOWNLOAD_RESOURCES_DIRECTORY = 'src/test/resources/s3-download-bucket'
    static final String UPLOAD_PROJECT_DIRECTORY = 'build/tmp/test/S3UploadTest'
    static final String UPLOAD_RESOURCES_DIRECTORY = 'src/test/resources/s3-upload-files'

    static final String DOWNLOAD_DIRECTORY_ROOT = 'download-dir-test'
    static final String DOWNLOAD_PATTERNS_ROOT = 'download-patterns-test'
    static final String SINGLE_DOWNLOAD_FILENAME = 'single-file.txt'

    static final String SINGLE_UPLOAD_FILENAME = 'single-file-upload.txt'
    static final String UPLOAD_DIRECTORY_NAME = 'directory-upload'

    static final String SINGLE_DIRECTORY_NAME = 'single-directory'
    static final String SINGLE_NESTED_DIRECTORY_NAME = 'single-directory/single-directory-subfolder'
    static final String SIMPLE_FILE_MATCHING_PATTERN = 'pattern-dir-1*'
    static final String COMPOUND_FILE_MATCHING_PATTERN = 'pattern-dir-2/pattern*'
    static final String MULTI_DIRECTORY_MATCHING_PATTERN = 'multi-match*'
    static final String DIRECTORY_MATCHING_PATTERN = 'single-dir*'

    static final String DEFAULT_REGION = 'us-east-1'

    static S3Client s3Client
    static String s3BucketName

    static File testProjectDir

    static String testKitParentDirectoryName
    static String testKitDownloadDirectoryName
    static String testKitUploadDirectoryName

    File downloadDir
    File patternsDir
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
     * Helper method to initialize the working directories in the Gradle test kit.
     */
    protected static void initializeTestKitDirectory(String name) {

        testKitParentDirectoryName = "${name}/${DAEMON_PREFIX}/${GRADLE_VERSION}"
        testKitDownloadDirectoryName = "${testKitParentDirectoryName}/${DOWNLOAD_DIRECTORY_PREFIX}"
        testKitUploadDirectoryName = "${testKitParentDirectoryName}/${UPLOAD_DIRECTORY_PREFIX}"

        List<String> folders = [testKitDownloadDirectoryName, testKitUploadDirectoryName]
        folders.each { folder ->
            File dir = new File(folder)
            if (dir.exists()) {
                dir.deleteDir()
            }
        }
    }

    /**
     * Helper method to derive the Gradle Test Kit root directory relative to the "fake" Gradle test project directory.
     */
    protected static File getTestKitRoot() {
        return testProjectDir.getCanonicalFile()
    }

    /**
     * Helper method to seed the "fake" Gradle test project root with files from the resources directory of the "real"
     * Gradle S3 Plugin project.
     */
    protected static String seedSingleUploadFile() {

        File projectDir = new File(UPLOAD_PROJECT_DIRECTORY)
        projectDir.mkdirs()

        File source = new File(UPLOAD_RESOURCES_DIRECTORY, SINGLE_UPLOAD_FILENAME)
        File target = new File(UPLOAD_PROJECT_DIRECTORY, SINGLE_UPLOAD_FILENAME)
        target << source.text

        return target.canonicalPath.replaceAll('\\\\', '\\/')
    }

    /**
     * Helper method to seed the "fake" Gradle test project root with files from the resources directory of the "real"
     * Gradle S3 Plugin project.
     */
    protected static List<String> seedDirectoryUploadFiles() {

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
            String prefix = file.parent.replace(parentRoot, '')
                    .replace(File.separator, '/')
                    .replaceAll(/(^\/)|(\/$)/, '')
            String key = prefix ? "${prefix}/${file.name}" : file.name

            PutObjectRequest request = PutObjectRequest.builder()
                                            .bucket(s3BucketName)
                                            .key(key)
                                            .build()
            s3Client.putObject(request, file.toPath())
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

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build()

        List<String> keys = s3Client.listObjectsV2(listRequest).contents()*.key()

        if (keys) {
            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                                                        .bucket(s3BucketName)
                                                        .delete(Delete.builder()
                                                                    .objects(keys.collect {
                                                                        ObjectIdentifier.builder().key(it).build()
                                                                    })
                                                                    .build())
                                                        .build()
            s3Client.deleteObjects(deleteRequest)
        }
    }

    /**
     * Helper method to delete the test S3 bucket.
     */
    protected static void deleteS3Bucket() {

        clearS3Bucket()
        DeleteBucketRequest request = DeleteBucketRequest.builder()
                                        .bucket(s3BucketName)
                                        .build()
        s3Client.deleteBucket(request)
    }

    /**
     * Helper method to reset the test project directory state and initialize the file handles used in the test run.
     */
    protected void setupProjectDirectoryFiles() {

        testProjectDir.mkdirs()

        buildFile = new File(testProjectDir, BUILD_FILE)
        settingsFile = new File(testProjectDir, SETTINGS_FILE)
        downloadDir = new File(testProjectDir, DOWNLOAD_DIRECTORY_ROOT)
        patternsDir = new File(testProjectDir, DOWNLOAD_PATTERNS_ROOT)

        [buildFile, settingsFile, downloadDir, patternsDir].each { File file ->
            if (file.exists()) {
                if (file.isDirectory()) {
                    file.deleteDir()
                }
                else {
                    file.delete()
                }
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
