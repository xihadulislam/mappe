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
    private static final MappeExecutor coroutineExecutor = new BackgroundCoroutineExecutor.CoroutineExecutor();

    public static Executor onMainThread() {
        return sMainThreadExecutor;
    }

    public static MappeExecutor onBackgroundThread() {
        if (executorTypeThread) {
            return new BackgroundThreadExecutor.BackgroundExecutor().withThreadPoolSize(defaultThreadPoolSize()).withTaskType("default");
        } else {
            return onCoroutineExecutor();
        }
    }

    public static MappeExecutor onFileDownloadBackgroundThread() {
        if (executorTypeThread) {
            return new BackgroundThreadExecutor.BackgroundExecutor().withThreadPoolSize(defaultThreadPoolSize()).withTaskType("file-downloading");
        } else {
            return onCoroutineExecutor();
        }
    }


    public static MappeExecutor onCoroutineExecutor() {
        return coroutineExecutor;
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
