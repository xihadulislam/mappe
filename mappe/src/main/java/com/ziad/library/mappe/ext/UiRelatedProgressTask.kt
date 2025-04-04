package com.ziad.library.mappe.ext

abstract class UiRelatedProgressTask<Result, Progress> : UiRelatedTask<Result>(), CancelableRunnable {
    /**
     * You can publish work progress and call it inside doWork().
     *
     * @param progress Progress that is passed to onProgressUpdate()
     * @see .onProgressUpdate
     */
    protected fun publishProgress(progress: Progress) {
        if (!isCanceled) {
            sUiHandler.post { onProgressUpdate(progress) }
        }
    }

    /**
     * Handles progress update. It is executed on the UI/main thread. Won't be called if `cancel()` is already called.
     *
     * @param progress Progress that was published via publishProgress()
     * @see .publishProgress
     * @see .cancel
     */
    protected abstract fun onProgressUpdate(progress: Progress)
}