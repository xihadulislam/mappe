package com.ziad.library.mappe.ext

abstract class AbstractCancelableRunnable : CancelableRunnable {
    @Volatile
    override var isCanceled: Boolean = false

    override fun cancel() {
        isCanceled = true
    }
}