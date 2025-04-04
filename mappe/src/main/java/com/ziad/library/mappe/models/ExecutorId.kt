package com.ziad.library.mappe.models

import java.util.Objects

/**
 * Created by @Ziad Islam on 13/02/2025.
 */

class ExecutorId(private val poolSize: Int, private val taskType: String) {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ExecutorId
        return poolSize == that.poolSize && taskType == that.taskType
    }

    override fun hashCode(): Int {
        return Objects.hash(poolSize, taskType)
    }
}