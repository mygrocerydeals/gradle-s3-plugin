package com.mgd.core.gradle

import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.transfer.Transfer
import org.gradle.api.logging.Logger

import java.text.DecimalFormat

/**
 * Progress listener for S3 actions. Used for logging completion status.
 */
class S3Listener implements ProgressListener {

    DecimalFormat df = new DecimalFormat('#0.0')
    Transfer transfer
    Logger logger

    S3Listener(Transfer transfer, Logger logger) {
        this.transfer = transfer
        this.logger = logger
    }

    void progressChanged(ProgressEvent e) {
        logger.info("${df.format(transfer.progress.percentTransferred)}%")
    }
}
