package com.mgd.core.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the S3 plugin extension with the Gradle build project.
 */
class S3Plugin implements Plugin<Project> {

    void apply(Project target) {
        target.extensions.create('s3', S3Extension)
    }
}
