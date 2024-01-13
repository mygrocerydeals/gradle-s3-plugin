# Gradle S3 Plugin
[![Install](https://img.shields.io/badge/install-plugin-brown.svg)](https://plugins.gradle.org/plugin/com.mgd.core.gradle.s3)
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE)
[![Gradle Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/mgd/core/gradle/s3/com.mgd.core.gradle.s3.gradle.plugin/maven-metadata.xml.svg?label=gradle)](https://plugins.gradle.org/plugin/com.mgd.core.gradle.s3)

Simple Gradle plugin that uploads and downloads S3 objects. This is a fork of the [mgk/s3-plugin](https://github.com/mgk/s3-plugin), which no longer appears to be under active development.
It has been updated to work with Gradle version 6 and later.

## Setup

Add the following to your build.gradle file:

```groovy
plugins {
    id 'com.mgd.core.gradle.s3' version '1.4.0'
}
```

## Versioning

This project uses [semantic versioning](http://semver.org)

See [gradle plugin page](https://plugins.gradle.org/plugin/com.mgd.core.gradle.s3) for other versions.

# Usage

## Authentication

The S3 plugin searches for credentials in the same order as the [AWS default credentials provider chain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html). See the [AWS Docs](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) for details on credentials.

### Profiles
You can specify a default credentials profile for the project to use by setting the project `s3.profile` property.
These credentials will be used if no other authentication mechanism has been specified for the Gradle task.

```groovy
s3 {
    profile = 'my-profile'
}
```

### Environment Variables

Setting the environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` is another way to provide your S3 credentials. Settings these variables
at the machine-level will make them available for every Gradle project to use as the default credentials.

### System Properties

Another way to set credentials is to set system properties at the task level. This may be useful for managing multiple tasks in the same project which each require distinct credentials.  
For example, suppose you want distinct tasks for uploading to different S3 buckets, each with different security credentials.
You could define different Gradle tasks (e.g. `uploadToS3Profile1`, `uploadToS3Profile2`) and map the credentials to each
using system properties:
```groovy
['Profile1', 'Profile2'].each { profile ->
    tasks.register("uploadToS3${profile}", S3Upload) {
        // credentials injected into the project as profile1KeyId / profile1SecretKey and profile2KeyId / profile2SecretKey 
        System.setProperty('aws.accessKeyId', project.ext."${profile.toLowerCase()}KeyId")
        System.setProperty('aws.secretKey', project.ext."${profile.toLowerCase()}SecretKey")

        bucket = 'target-bucketname'
        key = 'artifact.jar'
        file = layout.buildDirectory.file('libs/artifact.jar').get().asFile
        overwrite = true
    }
}
```

Note that this example is provided for illustrative purposes only. [All passwords should be externalized, secured via access control and/or encrypted.](https://docs.gradle.org/current/userguide/authoring_maintainable_build_scripts.html#sec:avoiding_passwords_in_plain_text)
A good option for managing secrets in build files is the [Gradle Credentials plugin](https://github.com/etiennestuder/gradle-credentials-plugin).

## Amazon EC2 Endpoint
The `s3.endpoint` property can be used to define an Amazon EC2 compatible third-party cloud environment for all tasks (e.g. LocalStack).
This option is only valid when combined with the `region` property (either defined globally using `s3.region` or defined for the task
using task-level properties). Endpoints can also be defined on a per-task basis.

```groovy
s3 {
    endpoint = 'http://localstack.cloud'
    region = 'global'
}
```

## Amazon EC2 Region

The `s3.region` property can optionally be set to define the Amazon EC2 region if one has not been set in the authentication profile.
It can also be used to override the default region set in the AWS credentials provider. Regions can also be defined on a per-task basis.

```groovy
s3 {
    region = 'us-east-1'
}
```

## Default S3 Bucket

The `s3.bucket` property sets a default S3 bucket that is common to all tasks. This can be useful if all S3 tasks operate against the same Amazon S3 bucket.

```groovy
s3 {
    bucket = 'my-default-bucketname'
}
```

## Tasks

The following Gradle tasks are provided.


### S3Upload

Uploads one or more files to S3. This task has two modes of operation: single file upload and directory upload (including recursive upload of all child subdirectories).  
  
Properties that apply to both modes:

  + `profile` - credentials profile to use *(optional, defaults to the project `s3` configured profile)*
  + `bucket` - S3 bucket to use *(optional, defaults to the project `s3` configured bucket, if any)*
  + `region` - the Amazon EC2 region *(optional, defaults to the project `s3` configured region, if any)*
  + `endpoint` - the third-party Amazon EC2 endpoint *(optional, defaults to the project `s3` configured endpoint, if any)*

#### Single file upload:

  + `key` - key of S3 object to create
  + `file` - path of file to be uploaded
  + `overwrite` - *(optional, default is `false`)*, if `true` the S3 object is created or overwritten if it already exists
  + `then` - *(optional)*, callback closure called upon completion with the java.io.File that was uploaded

By default `S3Upload` does not overwrite the S3 object if it already exists. Set `overwrite` to `true` to upload the file even if it exists.

#### Directory upload:

  + `keyPrefix` - root S3 prefix under which to create the uploaded contents *(optional, if not provided files will be uploaded to S3 bucket root)*
  + `sourceDir` - local directory containing the contents to be uploaded
  + `then` - *(optional)*, callback closure called upon completion with each java.io.File that was uploaded

A directory upload will always overwrite existing content if it already exists under the specified S3 prefix.

### S3Download

Downloads one or more S3 objects. This task has three modes of operation: single file
download, recursive download and path pattern matching.  
  
Properties that apply to all modes:

  + `profile` - credentials profile to use *(optional, defaults to the project `s3` configured profile)*
  + `bucket` - S3 bucket to use *(optional, defaults to the project `s3` configured bucket, if any)*
  + `region` - the Amazon EC2 region *(optional, defaults to the project `s3` configured region, if any)*
  + `endpoint` - the third-party Amazon EC2 endpoint *(optional, defaults to the project `s3` configured endpoint, if any)*

#### Single file download:

  + `key` - key of S3 object to download
  + `file` - local path of file to save the download to
  + `then` - *(optional)*, callback closure called upon completion with the java.io.File that was downloaded

#### Recursive download:

  + `keyPrefix` - S3 prefix of objects to download *(optional, if not provided entire S3 bucket will be downloaded)*
  + `destDir` - local directory to download objects to
  + `then` - *(optional)*, callback closure called upon completion with the java.io.File that was downloaded

#### Path pattern matching:
 
+ `pathPatterns` - a list of path patterns to match against, which can specify any combination of the following items:
  + an individual S3 object name (e.g. `/path/to/some-file.txt`)
  + a key prefix pointing to a folder (e.g. `/some-folder/`)  
    **NOTE:** when specifying folders, the folder name **must** end with a trailing forward slash, (i.e. `/`), otherwise it will be treated as an object name
  + a wildcard path pattern ending with an asterisk to search for matching folders (e.g. `/parent-folder/child-folder/folder-name-prefix-*`)
+ `destDir` - local directory to download objects into
+ `then` - *(optional)*, callback closure called upon completion with each java.io.File that was downloaded.

### Example:

```groovy
...

s3 {
    bucket = 'project-default-bucketname'
    endpoint = 'http://localstack.cloud'
    region = 'us-east-1'
}

tasks.register('defaultFilesDownload', S3Download) {
    keyPrefix = 'sourceFolder'
    destDir = 'targetDirectory'
}

tasks.register('singleFileDownload', S3Download) {
    bucket = 'task-source-bucketname'
    key = 'source-filename'
    file = 'target-filename'
}

tasks.register('downloadRecursive', S3Download) {
    keyPrefix = 'recursive/sourceFolder'
    destDir = './some/recursive/targetDirectory'
    then = { File file ->
        // do something with the file
    }
}

tasks.register('downloadPathPatterns', S3Download) {
    bucket = 'another-task-source-bucketname'
    pathPatterns = [
        'path/to/filename.txt',
        'single-folder/',
        'matching/folder/with-prefix-names*'
    ]
    destDir = 'pathPatternMatches'
    then = { File file ->
        // do something with the file
    }
}

tasks.register('filesUpload', S3Upload) {
    bucket = 'task-target-bucketname'
    keyPrefix = 'targetFolder'
    sourceDir = 'sourceDirectory'
}

tasks.register('defaultSingleFileUpload', S3Upload) {
    key = 'target-filename'
    file = 'source-filename'
}
```

***Note***:

Recursive downloads create a sparse directory tree containing the full `keyPrefix` under `destDir`. So with an S3 bucket
containing the object keys:

```
top/foo/bar
top/README
```

a recursive download:

```groovy
tasks.register('downloadRecursive', S3Download) {
  keyPrefix = 'top/foo/'
  destDir = 'local-dir'
}
```

results in this local tree:

```
local-dir/
└── top
    └── foo
        └── bar
```

So only files under `top/foo` are downloaded, but their full S3 paths are appended to the `destDir`. This is different from the behavior of the aws cli `aws s3 cp --recursive` command which prunes the root of the downloaded objects. Use the flexible [Gradle Copy](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Copy.html) task to prune the tree after downloading it.

For example:

```groovy
String s3PathTree = 'path/to/source/location'
String tempDownloadRoot = 'temp-download-root'

tasks.register('downloadRecursive', S3Download) {
    bucket = 's3-bucket-name'
    keyPrefix = "${s3PathTree}"
    destDir = layout.buildDirectory.dir(tempDownloadRoot).get().asFile
}

// prune and re-root the downloaded tree, removing the keyPrefix
tasks.register('pruneDownload', Copy) {

    dependsOn(tasks.downloadRecursive)

    from layout.buildDirectory.dir("${tempDownloadRoot}/${s3PathTree}")
    into layout.buildDirectory.dir('path/to/destination')
}
```

## Progress Reporting

Downloads report percentage progress at the gradle INFO level. Run gradle with the `-i` option to see download progress.

## License
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE)
