package com.ziad.library.mappe.executor


import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BackgroundCoroutineExecutor {

    private const val TAG = "Mappe"
    private const val DEFAULT_TASK_TYPE = "default"
    private var desiredTaskType = DEFAULT_TASK_TYPE
    private var job: Job = SupervisorJob()
    private var coroutineScope = CoroutineScope(job + Dispatchers.Default)
    private var executorExecuteCount = 0

    fun CoroutineScope.executeScope(runnable: Runnable) {
        this.launch {
            CoroutineExecutor().action(runnable)
        }
    }


    internal class CoroutineExecutor : MappeExecutor {

        override fun serially(): MappeExecutor {
            return withTaskType("serially")
        }

        override fun withTaskType(taskType: String): MappeExecutor {
            desiredTaskType = taskType
            return this
        }

        override fun withThreadPoolSize(poolSize: Int): MappeExecutor {
            require(poolSize >= 1) { "Thread pool size cannot be less than 1" }
            // In this case, poolSize is not used, consider removing this if unnecessary
            return this
        }

        override fun shutUp() {
            try {
                job.cancel() // Cancels the parent job
                coroutineScope.cancel() // Cancels all coroutines and cleans up resources
                Log.e(TAG, "All tasks cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Error while cancelling tasks: ${e.message}")
            } finally {
                executorExecuteCount = 0
            }
        }

        override fun isShutUp(): Boolean {
            return !coroutineScope.isActive
        }

        override fun execute(runnable: Runnable) {
            if (isShutUp()) reInit()
            coroutineScope.launch {
                action(runnable)
            }
        }

        suspend fun action(runnable: Runnable) = withContext(Dispatchers.IO) {
            try {
                runnable.run()
            } finally {
                executorExecuteCount++
                Log.d(TAG, "Executing task: $executorExecuteCount --> ${Thread.currentThread().name} -> ${Thread.activeCount()}")
            }
        }


        private fun reInit() {
            Log.w(TAG, "Reinitializing coroutine scope: ${Thread.activeCount()} --> ${Thread.currentThread().name}")
            job = SupervisorJob()
            coroutineScope = CoroutineScope(job + Dispatchers.Default)
            executorExecuteCount = 0
        }
    }
}