package com.atlassian.performance.tools.virtualusers.measure

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class JiraNodeCounter {

    private val counter: MutableMap<String, AtomicInteger> = ConcurrentHashMap()

    fun count(node: ApplicationNode) {
        val nodeId = node.identify()
        counter
            .computeIfAbsent(nodeId) { AtomicInteger() }
            .incrementAndGet()
    }

    fun dump(
        target: Appendable
    ) {
        val printer = CSVPrinter(target, CSVFormat.DEFAULT)
        getResults().forEach { entry ->
            printer.print(entry.key)
            printer.print(entry.value)
            printer.println()
            printer.flush()
        }
    }

    private fun getResults(): Map<String, Int> {
        return counter
            .mapValues { entry -> entry.value.toInt() }
    }

    override fun toString(): String {
        return getResults().toString()
    }
}
