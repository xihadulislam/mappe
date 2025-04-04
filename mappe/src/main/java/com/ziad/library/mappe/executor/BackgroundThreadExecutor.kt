package com.ziad.library.mappe.executor

import android.util.Log
import com.ziad.library.mappe.exception.CustomRejectedExecutionHandler
import com.ziad.library.mappe.exception.ExecutorShutdownException
import com.ziad.library.mappe.ext.CustomThreadFactory
import com.ziad.library.mappe.models.ExecutorId
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * BackgroundThreadExecutor for managing background tasks efficiently.
 * Created by @Ziad Islam on 13/02/2025.
 */
object BackgroundThreadExecutor {

    private const val TAG = "Mappe"
    private var desiredTaskType = "default"
    private const val KEEP_ALIVE_TIME = 60L
    private const val CLEANUP_INTERVAL = 60L
    private const val PAPI_REMOVE_INTERVAL = 0 // Adjust this value as necessary
    var desiredThreadPoolSize = Runtime.getRuntime().availableProcessors()
    private val cachedExecutors: MutableMap<ExecutorId, ThreadPoolExecutor> = HashMap()
    private val papiExecutors: MutableMap<ThreadPoolExecutor, Int> = HashMap()
    private var cleanupScheduler: ScheduledExecutorService? = null

    internal class BackgroundExecutor : MappeExecutor {

        init {
            initCleanup()
        }

        private fun initCleanup() {
            if (cleanupScheduler == null) {
                Log.w(TAG, "Initializing cleanup scheduler...")
                cleanupScheduler = Executors.newScheduledThreadPool(1).apply {
                    scheduleWithFixedDelay(
                        { cleanupIdleExecutors() },
                        CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.SECONDS
                    )
                }
            }
        }

        private fun shutdownCleanupScheduler() {
            try {
                cleanupScheduler?.shutdown()
                cleanupScheduler = null
                // Shutdown all cached executors
                cachedExecutors.values.forEach { shutdownExecutor(it) }
                cachedExecutors.clear()
                papiExecutors.clear()
            }catch (e:Exception){
                //
            }

        }

        override fun serially(): MappeExecutor {
            withThreadPoolSize(1)
            withTaskType("serially")
            return this
        }

        override fun withTaskType(taskType: String): MappeExecutor {
            desiredTaskType = taskType
            return this
        }

        override fun withThreadPoolSize(poolSize: Int): MappeExecutor {
            require(poolSize >= 1) { "Thread pool size cannot be less than 1" }
            desiredThreadPoolSize = poolSize
            return this
        }

        override fun execute(runnable: Runnable) {
            getExecutor().execute(runnable)
        }

        fun executeAndShutUp(runnable: Runnable) {
            val executor = getExecutor()
            if (executor.isShutdown) {
                throw ExecutorShutdownException("Executor is shut down.")
            }
            executor.execute(runnable)
            shutdownExecutor(executor) // Initiate shutdown after executing the task
        }

        override fun shutUp() {

            synchronized(this) {
                try {
                    cachedExecutors.values.forEach { shutdownExecutor(it) }
                    cachedExecutors.clear()
                    shutdownCleanupScheduler()
                }catch (e:Exception){
                    //
                }

            }

        }

        override fun isShutUp(): Boolean {
            return getExecutor().isShutdown
        }

        private fun getExecutor(): ThreadPoolExecutor {
            return synchronized(this) {
                val executorId = ExecutorId(desiredThreadPoolSize, desiredTaskType)
                val executor = cachedExecutors[executorId]

                if (executor != null && !executor.isShutdown && !executor.isTerminated) {
                    Log.d(TAG, "Reusing executor: $desiredTaskType -> active:${Thread.activeCount()} -> $executor")
                    return executor
                }
                shutdownExecutor(executor)
                // If the executor is null, shutdown, or terminated, create a new one
                val newExecutor = createThreadPoolExecutor(desiredThreadPoolSize)
                cachedExecutors[executorId] = newExecutor
                Log.d(TAG, "Creating new executor: $desiredTaskType -> pool size $desiredThreadPoolSize --> $newExecutor")
                initCleanup()
                newExecutor
            }
        }


//        private fun getExecutor(): ThreadPoolExecutor {
//            return synchronized(this) {
//                val executorId = ExecutorId(desiredThreadPoolSize, desiredTaskType)
//                cachedExecutors[executorId]?.also { executor ->
//                    Log.d(TAG, "Reusing executor:  $desiredTaskType -> active:${Thread.activeCount()} -> $executor")
//                } ?: run {
//                    val newExecutor = createThreadPoolExecutor(desiredThreadPoolSize)
//                    cachedExecutors[executorId] = newExecutor
//                    Log.d(TAG, "Creating new executor for $desiredTaskType --> $newExecutor")
//                    initCleanup()
//                    newExecutor
//                }
//            }
//        }

        private fun createThreadPoolExecutor(poolSize: Int): ThreadPoolExecutor {
            return ThreadPoolExecutor(
                poolSize,  // Core pool size
                poolSize,  // Maximum pool size
                KEEP_ALIVE_TIME,  // Keep-alive time for idle threads
                TimeUnit.SECONDS,  // Time unit for keep-alive time
                LinkedBlockingQueue(),  // Work queue
                CustomThreadFactory(desiredTaskType),  // Custom thread factory
                CustomRejectedExecutionHandler() // Custom handler for rejected tasks
            )
        }

        private fun cleanupIdleExecutors() {
            synchronized(this) {
                try {
                    Log.d(TAG, "Cleanup process started: ${cachedExecutors.size} executors.")
                    val iterator = cachedExecutors.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        val executor = entry.value
                        if (executor.isShutdown) {
                            Log.e(TAG, "Executor is already shut down, removing from cache.")
                            iterator.remove() // Remove from cache
                        } else if (executor.queue.isEmpty() && executor.activeCount == 0) {
                            shutdownExecutor(executor)
                            iterator.remove() // Remove from cache
                        } else {
                            cleanupPapiExecutors(executor)
                        }
                    }
                    if (cachedExecutors.isEmpty()) shutdownCleanupScheduler()
                }catch (e:Exception){
                    //
                }

            }
        }

        private fun cleanupPapiExecutors(executor: ThreadPoolExecutor): Boolean {
            return papiExecutors[executor]?.let { count ->
                if (count > PAPI_REMOVE_INTERVAL) {
                    shutdownExecutor(executor)
                    papiExecutors.remove(executor)
                    true
                } else {
                    Log.e(TAG, "Executor $executor has been active $count times.")
                    papiExecutors[executor] = count + 1
                    false
                }
            } ?: run {
                Log.e(TAG, "Adding new executor $executor to PAPI tracking.")
                papiExecutors[executor] = 1
                false
            }
        }

        private fun shutdownExecutor(executor: ThreadPoolExecutor?) {
            executor?.let {
                Thread {
                    Log.e(TAG, "Shutting down idle executor: $it")
                    it.shutdown()
                    try {
                        if (!it.awaitTermination(KEEP_ALIVE_TIME, TimeUnit.SECONDS)) {
                            Log.w(TAG, "Forcing shutdown of executor: $it")
                            it.shutdownNow()
                        }
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "Interrupted during shutdown, forcing shutdown: $it", e)
                        it.shutdownNow()
                        Thread.currentThread().interrupt()
                    }
                    Log.w(TAG, "Executor shut down successfully: $it")
                }.start()
            }
        }
    }
}