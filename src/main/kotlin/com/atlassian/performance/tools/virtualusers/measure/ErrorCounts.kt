package com.atlassian.performance.tools.virtualusers.measure

import net.jcip.annotations.NotThreadSafe
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

@NotThreadSafe
data class ErrorCounts(
    private val errors: MutableMap<String, Int> = mutableMapOf()
) {

    fun add(label: String, amount: Int = 1) {
        errors[label] = errors.getOrPut(label) { 0 } + amount
    }

    fun merge(other: ErrorCounts): ErrorCounts {
        val copy = copy()
        other.errors.forEach { copy.add(it.key, it.value) }
        return copy
    }

    fun dump(target: Appendable) {
        val printer = CSVPrinter(target, CSVFormat.DEFAULT)
        errors.forEach { entry ->
            printer.print(entry.key)
            printer.print(entry.value)
            printer.println()
            printer.flush()
        }
    }
}