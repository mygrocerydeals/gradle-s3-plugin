package com.mgd.core.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the S3 plugin extension with the Gradle build project and defines properties for the upload and download
 * tasks.
 */
class S3Plugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create('s3', S3Extension)
        project.extensions.extraProperties.set('S3Download', S3Download)
        project.extensions.extraProperties.set('S3Upload', S3Upload)
    }
}
