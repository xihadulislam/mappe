package com.ziad.library.mappe.executor

import java.util.concurrent.Executor

/**
 * Created by @Ziad Islam on 13/02/2025.
 */

interface MappeExecutor : Executor {

    fun serially(): MappeExecutor

    fun withTaskType(taskType: String): MappeExecutor

    fun withThreadPoolSize(poolSize: Int): MappeExecutor

    fun withDelay(delayMs: Long): MappeExecutor

    fun executeAndShutUp(runnable: Runnable) {
        execute(runnable)
        shutUp()
    }

    fun shutUp()

    fun isShutUp(): Boolean
}
