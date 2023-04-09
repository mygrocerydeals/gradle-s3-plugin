package com.mgd.core.gradle

import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressEventType
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.transfer.Download

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
