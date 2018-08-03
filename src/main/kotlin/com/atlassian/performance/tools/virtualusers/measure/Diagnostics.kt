package com.atlassian.performance.tools.virtualusers.measure

interface Diagnostics {

    /**
     * @return diagnosis
     */
    fun diagnose(exception: Exception): String
}