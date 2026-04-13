package com.ziad.library.mappe;

import com.ziad.library.mappe.executor.BackgroundCoroutineExecutor;
import com.ziad.library.mappe.executor.BackgroundThreadExecutor;
import com.ziad.library.mappe.executor.MainThreadExecutor;
import com.ziad.library.mappe.executor.MappeExecutor;

import java.util.concurrent.Executor;

/**
 * Created by @Ziad Islam on 14/02/2025. happy valentine's day
 */

public class Mappe {

    // TODO: 2/18/2025   Do not write anything here without asking me (@Ziad Islam)

    private static final Boolean executorTypeThread = true; // thread = true / coroutine = false
    private static final Executor sMainThreadExecutor = new MainThreadExecutor.MainExecutor();

    public static Executor onMainThread() {
        return sMainThreadExecutor;
    }

    // ── Generic entry point ──────────────────────────────────────────────────
    // Use any name to get (or reuse) a dedicated pool for that task type.
    // Two calls with the same name share the same underlying pool.
    //
    //   Mappe.on("payment").execute(...)
    //   Mappe.on("sync-contacts").execute(...)
    // ─────────────────────────────────────────────────────────────────────────
    public static MappeExecutor on(String taskType) {
        if (executorTypeThread) {
            return BackgroundThreadExecutor.createExecutor()
                    .withThreadPoolSize(defaultThreadPoolSize())
                    .withTaskType(taskType);
        } else {
            return BackgroundCoroutineExecutor.createExecutor()
                    .withThreadPoolSize(defaultThreadPoolSize())
                    .withTaskType(taskType);
        }
    }

    // ── Named convenience methods (delegate to on()) ──────────────────────────
    public static MappeExecutor onBackgroundThread() {
        return on("Default");
    }

    public static MappeExecutor onIOBackgroundThread() {
        return on("I/O");
    }

    public static MappeExecutor onAllBackgroundThread() {
        return on("All");
    }

    public static MappeExecutor onBulkBackgroundThread() {
        return on("Bulk");
    }

    public static MappeExecutor onLogBackgroundThread() {
        return on("Log");
    }

    public static MappeExecutor onSocketBackgroundThread() {
        return on("Socket");
    }

    public static MappeExecutor onPrintBackgroundThread() {
        return on("Print");
    }

    public static MappeExecutor onFileDownloadBackgroundThread() {
        return on("File-downloading");
    }

    public static StringBuilder getThreadReport() {
        return BackgroundThreadExecutor.getThreadReport();
    }

    public static StringBuilder getCoroutineReport() {
        return BackgroundCoroutineExecutor.getCoroutineReport();
    }


    private static volatile Integer threadPoolSize = null; // Use volatile for thread safety
    private static final Object lock = new Object(); // Lock object for synchronization

    private static Integer defaultThreadPoolSize() {
        if (threadPoolSize == null) {
            synchronized (lock) {
                if (threadPoolSize == null) { // Double-check to avoid multiple initializations
                    threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
                }
            }
        }
        return threadPoolSize;
    }


}
