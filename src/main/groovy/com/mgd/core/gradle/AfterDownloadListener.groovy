package com.mgd.core.gradle

import software.amazon.awssdk.transfer.s3.model.Download
import software.amazon.awssdk.transfer.s3.progress.TransferListener

/**
 * File download completion event callback event delegate.
 */
class AfterDownloadListener implements ProgressListener {

    Download download
    File dest
    Closure<Void> then

    AfterDownloadListener(Download download, File dest, Closure<Void> then) {
        this.download = download
        this.dest = dest
        this.then = then
    }

    @Override
    void progressChanged(ProgressEvent e) {
        if (e.eventType == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
            then?.call(new File(dest, download.key))
        }
    }
}
