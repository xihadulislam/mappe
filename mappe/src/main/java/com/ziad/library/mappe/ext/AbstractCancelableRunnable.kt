package com.ziad.library.mappe.ext

abstract class AbstractCancelableRunnable : CancelableRunnable {
    override var isCanceled: Boolean = false

    override fun cancel() {
        isCanceled = true
    }
}