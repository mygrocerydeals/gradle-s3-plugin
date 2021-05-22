package com.fuseanalytics.gradle.s3

import com.amazonaws.event.ProgressEventType
import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.transfer.model.UploadResult

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
        if( e.eventType == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
            UploadResult result = upload.waitForUploadResult()
            then?.call( new File( dest, result.key ) )
        }
    }

}
