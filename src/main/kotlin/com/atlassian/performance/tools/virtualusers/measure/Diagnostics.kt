package com.atlassian.performance.tools.virtualusers.measure

internal interface Diagnostics {

    /**
     * @return diagnosis
     */
    fun diagnose(exception: Exception): String
}