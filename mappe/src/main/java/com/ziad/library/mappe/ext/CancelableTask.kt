package com.ziad.library.mappe.ext

import com.ziad.library.mappe.ext.AbstractCancelableRunnable
import com.ziad.library.mappe.ext.CancelableRunnable

abstract class CancelableTask : AbstractCancelableRunnable(), CancelableRunnable {
    override fun run() {
        if (!isCanceled) {
            doWork()
        }
    }

    /**
     * Defines the task that will be executed. Won't be called if `cancel()` is already called.
     *
     * @see .cancel
     */
    protected abstract fun doWork()
}