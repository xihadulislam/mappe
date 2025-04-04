package com.ziad.library.mappe.ext

import android.os.Handler
import android.os.Looper
import android.os.Process


abstract class UiRelatedTask<Result> : AbstractCancelableRunnable(), CancelableRunnable {
    override fun run() {
        if (!isCanceled) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            val result = doWork()
            if (!isCanceled) {
                sUiHandler.post {
                    if (!isCanceled) {
                        thenDoUiRelatedWork(result)
                    }
                }
            }
        }
    }

    protected abstract fun doWork(): Result

    protected abstract fun thenDoUiRelatedWork(result: Result)

    companion object {
        var sUiHandler: Handler = Handler(Looper.getMainLooper())
    }
}