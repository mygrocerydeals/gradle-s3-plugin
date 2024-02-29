package com.mgd.core.gradle

/**
 * File transfer completion event callback event delegate.
 */
class AfterTransferListener {

    File file
    Closure<Void> then

    AfterTransferListener(File file, Closure<Void> then) {
        this.file = file
        this.then = then
    }

    void transferComplete() {
        then.call(file)
    }
}
