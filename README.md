# Gradle S3 Plugin
[![Install](https://img.shields.io/badge/install-plugin-brown.svg)](https://plugins.gradle.org/plugin/com.mgd.core.gradle.s3)
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE)
[![Gradle Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/mgd/core/gradle/s3/com.mgd.core.gradle.s3.gradle.plugin/maven-metadata.xml.svg?label=gradle)](https://plugins.gradle.org/plugin/com.mgd.core.gradle.s3)

Simple Gradle plugin that uploads and downloads S3 objects. It is designed to work with Gradle version 7 and later. 

## Setup

Add the following to your build.gradle file:

```groovy
plugins {
    id 'com.mgd.core.gradle.s3' version '2.1.2'
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

Setting the environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` is another way to provide your S3 credentials.
Settings these variables at the machine-level will make them available for every Gradle project to use as the default
credentials. The environment variables can also be set or overridden at the task level for each Gradle task which requires them.


### System Properties

Another way to set S3 credentials is to set system properties at the Gradle task level. This may be useful for managing multiple
tasks in the same project which each require distinct credentials.  
For example, suppose you want distinct tasks for uploading to different S3 buckets, each with different security credentials.
You could define different Gradle tasks (e.g. `uploadToS3Profile1`, `uploadToS3Profile2`) and map the credentials to each
using the AWS SDK v2 system properties:
```groovy
['Profile1', 'Profile2'].each { profile ->
    tasks.register("uploadToS3${profile}", S3Upload) {
        // credentials injected into the project as profile1KeyId, profile1SecretAccessKey and profile2KeyId, profile2SecretAccessKey 
        System.setProperty('aws.accessKeyId', project.ext."${profile.toLowerCase()}KeyId")
        System.setProperty('aws.secretAccessKey', project.ext."${profile.toLowerCase()}SecretAccessKey")

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
The `s3.endpoint` property can be used to define an Amazon EC2 compatible third-party cloud or Docker environment for all tasks (e.g. LocalStack).
This option is only valid when combined with the `region` property (either defined globally using `s3.region` or defined for the task
using task-level properties). Endpoints can also be defined on a per-task basis, which enables switching between Amazon S3 and third-party
endpoints for each task, if needed.

```groovy
s3 {
    endpoint = 'http://localstack.cloud'
    region = 'global'
}
```

## S3 Path-Style URLs

[Amazon has deprecated path-style URLs for the S3 APIs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html#path-style-access). However, some third-party S3-compatible object storage providers specified using `s3.endpoint` may not support virtual-hosted style addressing (e.g. Oracle's Object Storage).
The `s3.usePathStyleUrl` property can be used to default path-style S3 URls for all third-party provider tasks. This property can also be defined on a per-task basis.
When defined at the global level, the property is ignored for any tasks which target native Amazon S3 buckets (which always use
the default, virtual-hosted style URLs).

```groovy
s3 {
  endpoint = 'https://my-tenancy.compat.objectstorage.us-chicago-1.oraclecloud.com'
  region = 'global'
  usePathStyleUrl = true
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
+ `usePathStyleUrl` - whether to use path-style URLs for accessing third-party endpoints *(optional, defaults to the project `s3` configured value and ignored if `endpoint` is not set for the task)*

#### Single file upload:

+ `key` - key of S3 object to create
+ `file` - path of file to be uploaded
+ `contentType` - the content type of the file being uploaded *(optional, s3 will default to 
  `application/octet-stream`)*
+ `overwrite` - *(optional, default is `false`)*, if `true` the S3 object is created or overwritten if it already exists
+ `then` - *(optional)*, callback closure called upon completion with the java.io.File that was uploaded

By default `S3Upload` does not overwrite the S3 object if it already exists. Set `overwrite` to `true` to upload the file even if it exists.

#### Directory upload:

+ `keyPrefix` - root S3 prefix under which to create the uploaded contents *(optional, if not provided files will be uploaded to S3 bucket root)*
+ `sourceDir` - local directory containing the contents to be uploaded

A directory upload will always overwrite existing content if it already exists under the specified S3 prefix.

### S3Download

Downloads one or more S3 objects. This task has three modes of operation: single file
download, recursive download and path pattern matching.  
  
Properties that apply to all modes:

+ `profile` - credentials profile to use *(optional, defaults to the project `s3` configured profile)*
+ `bucket` - S3 bucket to use *(optional, defaults to the project `s3` configured bucket, if any)*
+ `region` - the Amazon EC2 region *(optional, defaults to the project `s3` configured region, if any)*
+ `endpoint` - the third-party Amazon EC2 endpoint *(optional, defaults to the project `s3` configured endpoint, if any)*
+ `usePathStyleUrl` - whether to use path-style URLs for accessing third-party endpoints *(optional, defaults to the project `s3` configured value and ignored if `endpoint` is not set for the task)*

#### Single file download:

+ `key` - key of S3 object to download
+ `file` - local path of file to save the download to
+ `version` - *(optional)*, the specific object version id to download if the bucket has S3 versioning enabled
  + if S3 versioning is not enabled, this field should **not** be provided
  + if S3 versioning is enabled and a version id is not provided, the latest file will be always be downloaded  
  + discovery of S3 object version ids must be accomplished via other means and is beyond the scope of this plugin  
    **NOTE:** care should be exercised when using this parameter, as incorrect values will cause the task to fail  
+ `then` - *(optional)*, callback closure called upon completion with the java.io.File that was downloaded

#### Recursive download:

+ `keyPrefix` - S3 prefix of objects to download *(optional, if not provided entire S3 bucket will be downloaded)*
+ `destDir` - local directory to download objects to

#### Path pattern matching:
 
+ `pathPatterns` - a list of path patterns to match against, which can specify any combination of the following items:
  + an individual S3 object name (e.g. `/path/to/some-file.txt`)
  + a key prefix pointing to a folder (e.g. `/some-folder/`)  
    **NOTE:** when specifying folders, the folder name **must** end with a trailing forward slash, (i.e. `/`), otherwise it will be treated as an object name
  + a wildcard path pattern ending with an asterisk to search for matching folders (e.g. `/parent-folder/child-folder/folder-name-prefix-*`)
+ `destDir` - local directory to download objects into
+ `then` - *(optional, invoked only on individual S3 object name patterns)*, callback closure called upon completion with the java.io.File that was downloaded

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
    then = { File file ->
        // do something with the file
        println("Downloaded file named ${file.name}!")
    }
}

tasks.register('downloadRecursive', S3Download) {
    keyPrefix = 'recursive/sourceFolder'
    destDir = './some/recursive/targetDirectory'
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
        println("Downloaded the file named 'path/to/filename.txt' to ${file.parent}!")
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
