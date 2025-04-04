package com.ziad.library.mappe.ext

interface CancelableRunnable : Runnable {
    fun cancel()

    val isCanceled: Boolean
}