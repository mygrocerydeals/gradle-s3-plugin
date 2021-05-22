# Gradle S3 Plugin
[![Install](https://img.shields.io/badge/install-plugin-brown.svg)](https://plugins.gradle.org/plugin/com.fuseanalytics.gradle.s3)
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE)
[![Gradle Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/fuseanalytics/gradle/s3/com.fuseanalytics.gradle.s3/maven-metadata.xml.svg?label=gradle)](Gradle Plugin Version)

Simple Gradle plugin that uploads and downloads S3 objects. This is a fork of 
the [gradle-s3-plugin](https://github.com/mygrocerydeals/gradle-s3-plugin) which 
is a fork of [mgk/s3-plugin](https://github.com/mgk/s3-plugin), which both  
appear to be no longer under active development.  It has been updated to work with 
Gradle version 6 and later.

## Setup

Add the following to your build.gradle file:

```groovy
plugins {
  id 'com.fuseanalytics.gradle.s3' version '1.1.3'
}
```

## Versioning

This project uses [semantic versioning](http://semver.org)

See [gradle plugin page](https://plugins.gradle.org/plugin/com.mgd.core.gradle.s3) for other versions.

# Usage

## Authentication

The S3 plugin searches for credentials in the same order as the [AWS default credentials provider chain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html). Additionally you can specify a credentials profile to use by setting the project `s3.profile` property:

```groovy
s3 {
    profile = 'my-profile'
}
```

Setting the environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` is one way to provide your S3 credentials. See the [AWS Docs](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) for details on credentials.

## Amazon EC2 Region

The `s3.region` property can optionally be set to define the Amazon EC2 region if one has not been set in the authentication profile. It can also be used to override the default region set in the AWS credentials provider. 

```groovy
s3 {
    region = 'us-east-1'
}
```

## Default S3 Bucket

The `s3.bucket` property sets a default S3 bucket that is common to all tasks. This can be useful if all S3 tasks operate against the same Amazon S3 bucket.

```groovy
s3 {
    bucket = 'my.default.bucketname'
}
```

## Tasks

The following Gradle tasks are provided.


### S3Upload

Uploads one or more files to S3. This task has two modes of operation: single file upload and directory upload (including recursive upload of all child subdirectories). Properties that apply to both modes:

  + `bucket` - S3 bucket to use *(optional, defaults to the project `s3` configured bucket)*

For a single file upload:

  + `key` - key of S3 object to create
  + `file` - path of file to be uploaded
  + `overwrite` - *(optional, default is `false`)*, if `true` the S3 object is created or overwritten if it already exists.
  + `then` - *(optional)*, callback closure called upon completion with the java.io.File that was uploaded.

By default `S3Upload` does not overwrite the S3 object if it already exists. Set `overwrite` to `true` to upload the file even if it exists.

For a directory upload:

  + `keyPrefix` - root S3 prefix under which to create the uploaded contents
  + `sourceDir` - local directory containing the contents to be uploaded
  + `then` - *(optional)*, callback closure called upon completion with each java.io.File that was uploaded.

A directory upload will always overwrite existing content if it already exists under the specified S3 prefix.

### S3Download

Downloads one or more S3 objects. This task has two modes of operation: single file
download and recursive download. Properties that apply to both modes:

  + `bucket` - S3 bucket to use *(optional, defaults to the project `s3` configured bucket)*

For a single file download:

  + `key` - key of S3 object to download
  + `file` - local path of file to save the download to
  + `then` - *(optional)*, callback closure called upon completion with the java.io.File that was downloaded.

For a recursive download:

  + `keyPrefix` - S3 prefix of objects to download
  + `destDir` - local directory to download objects to
  + `then` - *(optional)*, callback closure called upon completion with each java.io.File that was downloaded.

***Note***:  
  
Recursive downloads create a sparse directory tree containing the full `keyPrefix` under `destDir`. So with an S3 bucket
containing the object keys:

```
top/foo/bar
top/README
```

a recursive download:

```groovy
task downloadRecursive(type: S3Download) {
  keyPrefix = 'top/foo/'
  destDir = 'local-dir'
  then = { File file ->
      // do something with the file
  }  
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
def localTree = 'path/to/some/location'

task downloadRecursive(type: S3Download) {
    bucket = 's3-bucket-name'
    keyPrefix = "${localTree}"
    destDir = "${buildDir}/download-root"
}

// prune and re-root the downloaded tree, removing the keyPrefix
task copyDownload(type: Copy, dependsOn: downloadRecursive) {
    from "${buildDir}/download-root/${localTree}"
    into "${buildDir}/pruned-tree"
}
```

## Progress Reporting

Downloads report percentage progress at the gradle INFO level. Run gradle with the `-i` option to see download progress.

## License
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE)
