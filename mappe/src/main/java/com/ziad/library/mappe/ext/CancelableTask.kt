package com.ziad.library.mappe.ext

abstract class CancelableTask : AbstractCancelableRunnable() {
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