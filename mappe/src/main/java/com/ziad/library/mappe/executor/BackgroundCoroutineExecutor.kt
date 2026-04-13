package com.ziad.library.mappe.executor

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * BackgroundCoroutineExecutor — Channel-based worker pool for high-throughput coroutine dispatch.
 *
 * Architecture mirrors BackgroundThreadExecutor:
 * - WorkerPool (channel + N workers) keyed by CoroutineExecutorId
 * - Fixed N workers consume from a bounded Channel<Runnable> — memory footprint is O(N + capacity),
 *   not O(submitted tasks). Safe for millions of concurrent callers.
 * - Bounded channel = natural backpressure; tasks rejected when full (logged + counted).
 * - withThreadPoolSize() controls the actual number of worker coroutines.
 * - Per-pool metrics: completed, rejected, active counts.
 * - CoroutineExceptionHandler catches any uncaught exception escaping a worker.
 * - Graceful shutdown: close channel → workers drain → scope cancelled.
 *
 * Created by @Ziad Islam on 13/02/2025.
 */
object BackgroundCoroutineExecutor {

    private const val TAG = "Mappe"
    private const val DEFAULT_TASK_TYPE = "default"
    private const val DEFAULT_CHANNEL_CAPACITY = 1_000

    private val workerPools = ConcurrentHashMap<CoroutineExecutorId, WorkerPool>()

    // ══════════════════════════════════════════
    //  CoroutineExecutorId — pool key
    // ══════════════════════════════════════════

    internal data class CoroutineExecutorId(val workerCount: Int, val taskType: String)

    // ══════════════════════════════════════════
    //  WorkerPool — mirrors ExecutorWrapper
    // ══════════════════════════════════════════

    internal class WorkerPool(val workerCount: Int, val taskType: String) {

        val completedCount = AtomicLong(0)
        val rejectedCount = AtomicLong(0)
        val activeCount = AtomicInteger(0)

        private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "[$taskType] Uncaught exception in worker coroutine", throwable)
        }

        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO + exceptionHandler)
        val channel = Channel<Runnable>(DEFAULT_CHANNEL_CAPACITY)

        init {
            repeat(workerCount) { workerId ->
                scope.launch {
                    for (task in channel) {
                        activeCount.incrementAndGet()
                        try {
                            task.run()
                            completedCount.incrementAndGet()
                        } catch (e: CancellationException) {
                            // Re-throw so the coroutine cancels properly
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "[$taskType] Worker $workerId: task failed", e)
                        } finally {
                            activeCount.decrementAndGet()
                        }
                    }
                    Log.d(TAG, "[$taskType] Worker $workerId stopped")
                }
            }
            Log.d(TAG, "[$taskType] WorkerPool ready — $workerCount workers, capacity=$DEFAULT_CHANNEL_CAPACITY")
        }

        fun isHealthy(): Boolean = scope.isActive && !channel.isClosedForSend

        /**
         * Non-blocking submit. Returns false and logs if the channel is full.
         */
        fun submit(runnable: Runnable): Boolean {
            val result = channel.trySend(runnable)
            return if (result.isSuccess) {
                true
            } else {
                rejectedCount.incrementAndGet()
                Log.w(
                    TAG, "[$taskType] Channel full (capacity=$DEFAULT_CHANNEL_CAPACITY) — " +
                            "task rejected. Total rejected: ${rejectedCount.get()}"
                )
                false
            }
        }

        /**
         * Graceful shutdown: close channel so workers drain remaining tasks, then cancel scope.
         */
        fun shutdown() {
            channel.close()
            scope.cancel()
            Log.d(
                TAG, "[$taskType] Shutdown — " +
                        "completed=${completedCount.get()}, " +
                        "rejected=${rejectedCount.get()}, " +
                        "active=${activeCount.get()}"
            )
        }
    }

    // ══════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════

    @JvmStatic
    fun createExecutor(): MappeExecutor = CoroutineExecutor()

    @JvmStatic
    fun getCoroutineReport(): StringBuilder {
        val report = StringBuilder()
        report.appendLine("Coroutine Worker Pools: ${workerPools.size}")
        report.appendLine()
        workerPools.forEach { (id, pool) ->
            val status = if (pool.isHealthy()) "✅ ACTIVE" else "❌ UNHEALTHY"
            report.appendLine(
                "$status | ${id.taskType} | Workers: ${id.workerCount} | " +
                        "Active: ${pool.activeCount.get()} | " +
                        "Completed: ${pool.completedCount.get()} | " +
                        "Rejected: ${pool.rejectedCount.get()}"
            )
            report.appendLine()
        }
        return report
    }

    // ══════════════════════════════════════════
    //  CoroutineExecutor (instance logic only)
    // ══════════════════════════════════════════

    internal class CoroutineExecutor : MappeExecutor {

        private var instanceWorkerCount = Runtime.getRuntime().availableProcessors() * 2
        private var instanceTaskType = DEFAULT_TASK_TYPE
        private var instanceDelay: Long = 0

        // ── Hot-path cache: skip ConcurrentHashMap lookup on repeated calls ──
        @Volatile private var cachedPool: WorkerPool? = null
        @Volatile private var cachedId: CoroutineExecutorId? = null

        override fun serially(): MappeExecutor {
            instanceWorkerCount = 1
            return withTaskType("serially")
        }

        override fun withTaskType(taskType: String): MappeExecutor {
            instanceTaskType = taskType
            invalidateCache()
            return this
        }

        override fun withThreadPoolSize(poolSize: Int): MappeExecutor {
            require(poolSize >= 1) { "Thread pool size cannot be less than 1" }
            instanceWorkerCount = poolSize
            invalidateCache()
            return this
        }

        override fun withDelay(delayMs: Long): MappeExecutor {
            require(delayMs >= 0) { "Delay cannot be negative" }
            instanceDelay = delayMs
            return this
        }

        private fun invalidateCache() {
            cachedPool = null
            cachedId = null
        }

        override fun execute(runnable: Runnable) {
            if (instanceDelay > 0) {
                // Launch a lightweight timer coroutine; doesn't block a thread during the delay
                val delayMs = instanceDelay
                val pool = resolvePool()
                pool.scope.launch {
                    delay(delayMs)
                    pool.submit(runnable)
                }
                return
            }
            resolvePool().submit(runnable)
        }

        override fun executeAndShutUp(runnable: Runnable) {
            // Submit the task, close the channel so no new tasks are accepted,
            // then let workers drain naturally before the scope is cancelled.
            val pool = resolvePool()
            pool.submit(runnable)
            pool.channel.close()
            workerPools.remove(getOrCreateId())
            invalidateCache()
        }

        override fun shutUp() {
            val id = cachedId ?: getOrCreateId()
            workerPools.remove(id)?.shutdown()
            invalidateCache()
        }

        override fun isShutUp(): Boolean {
            val pool = cachedPool
            if (pool != null) return !pool.isHealthy()
            return workerPools[getOrCreateId()]?.isHealthy()?.not() ?: true
        }

        // ── Pool Resolution ──

        private fun getOrCreateId(): CoroutineExecutorId {
            var id = cachedId
            if (id == null || id.workerCount != instanceWorkerCount || id.taskType != instanceTaskType) {
                id = CoroutineExecutorId(instanceWorkerCount, instanceTaskType)
                cachedId = id
            }
            return id
        }

        private fun resolvePool(): WorkerPool {
            // ── FAST PATH ──
            val cached = cachedPool
            if (cached != null && cached.isHealthy()) return cached

            val id = getOrCreateId()

            // ── LOCK-FREE READ ──
            val existing = workerPools[id]
            if (existing != null && existing.isHealthy()) {
                cachedPool = existing
                return existing
            }

            // ── SLOW PATH: create or replace pool ──
            val pool = workerPools.compute(id) { _, current ->
                when {
                    current != null && current.isHealthy() -> {
                        Log.v(TAG, "Reusing coroutine pool: ${id.taskType} -> workers:${id.workerCount}")
                        current
                    }
                    else -> {
                        if (current != null) {
                            Log.w(TAG, "Coroutine pool unhealthy, replacing: ${id.taskType}")
                            current.shutdown()
                        } else {
                            Log.d(TAG, "Creating coroutine pool: ${id.taskType} -> workers:${id.workerCount}")
                        }
                        WorkerPool(id.workerCount, id.taskType)
                    }
                }
            }!!

            cachedPool = pool
            return pool
        }
    }
}
