package com.ziad.library.mappe.ext

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class CustomThreadFactory(private val taskType: String) : ThreadFactory {

    companion object {
        private const val TAG = "Mappe"
    }

    private val threadCount = AtomicInteger(1)
    private val group: ThreadGroup? = System.getSecurityManager()?.threadGroup ?: Thread.currentThread().threadGroup

    override fun newThread(r: Runnable): Thread {
        return Thread(group, r, "$TAG-$taskType-Thread-${threadCount.getAndIncrement()}").apply {
            isDaemon = false // Ensure threads are non-daemon
            priority = Thread.NORM_PRIORITY // Set default priority
        }
    }
}

//class EnhancedThreadFactory(private val taskType: String) : ThreadFactory {
//
//    private val threadCount = AtomicInteger(1)
//    private val group: ThreadGroup? = System.getSecurityManager()?.threadGroup ?: Thread.currentThread().threadGroup
//
//    companion object {
//        private const val TAG = "EnhancedThreadFactory"
//    }
//
//    override fun newThread(r: Runnable): Thread {
//        return Thread(group, r, "$TAG-$taskType-Thread-${threadCount.getAndIncrement()}").apply {
//            isDaemon = false // Ensure threads are non-daemon
//            priority = Thread.NORM_PRIORITY // Set default priority
//        }
//    }
//}