package com.mgd.core.gradle

import org.gradle.api.logging.Logger

/**
 * File transfer completion event callback event delegate.
 */
class AfterTransferListener {

    File file
    Closure<Void> then
    Logger logger

    AfterTransferListener(File file, Logger logger, Closure<Void> then) {
        this.file = file
        this.then = then
        this.logger = logger
    }

    void transferComplete() {
        try {
            then.call(file)
        }
        catch (Exception e) {
            logger.warn("Exception thrown in file callback for file ${file?.name}: ${e.message}", e)
        }
    }
}
