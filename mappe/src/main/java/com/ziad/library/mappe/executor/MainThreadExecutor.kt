package com.ziad.library.mappe.executor

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Created by @Ziad Islam on 13/02/2025.
 */

object MainThreadExecutor {

    internal class MainExecutor : Executor {
        private val mHandler = Handler(Looper.getMainLooper())

        override fun execute(runnable: Runnable) {
            mHandler.post(runnable)
        }
    }

}

