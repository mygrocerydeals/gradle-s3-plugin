package com.mgd.core.gradle

import org.gradle.api.logging.Logger
import software.amazon.awssdk.transfer.s3.progress.TransferListener

import java.text.DecimalFormat

/**
 * Progress listener for S3 actions. Used for logging completion status.
 */
@SuppressWarnings('UnusedMethodParameter')
class S3Listener implements TransferListener {

    DecimalFormat df = new DecimalFormat('#0.0')
    Logger logger
    AfterTransferListener transferListener

    S3Listener(Logger logger, AfterTransferListener transferListener = null) {
        this.logger = logger
        this.transferListener = transferListener
    }

    void transferInitiated(Context.TransferInitiated context) {}

    void transferFailed(Context.TransferFailed context) {
        Throwable e = context.exception()
        logger.warn("Transfer failed: ${e.message}")
    }

    void transferComplete(Context.TransferComplete context) {
        if (transferListener) {
            transferListener.transferComplete()
        }
    }

    void bytesTransferred(Context.BytesTransferred context) {
        double ratio = context.progressSnapshot().ratioTransferred().orElse(0)
        logger.info("${df.format(ratio * 100)}%")
    }
}
