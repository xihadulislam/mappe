package com.ziad.library.mappe.exception

import android.util.Log
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

class CustomRejectedExecutionHandler : RejectedExecutionHandler {
    companion object {
        private const val TAG = "Mappe"
    }

    override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
        Log.d(TAG, "Task $r rejected from $executor")
    }
}