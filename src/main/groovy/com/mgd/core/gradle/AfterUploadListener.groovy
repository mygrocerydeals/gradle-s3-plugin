package com.mgd.core.gradle

import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressEventType
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult

/**
 * File upload completion event callback event delegate.
 */
class AfterUploadListener implements ProgressListener {

    Upload upload
    File dest
    Closure<Void> then

    AfterUploadListener(Upload upload, File dest, Closure<Void> then) {
        this.upload = upload
        this.dest = dest
        this.then = then
    }

    void progressChanged(ProgressEvent e) {
        if (e.eventType == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
            UploadResult result = upload.waitForUploadResult()
            then?.call(new File(dest, result.key))
        }
    }
}
