package com.atlassian.performance.tools.virtualusers.measure

import com.atlassian.performance.tools.virtualusers.api.diagnostics.Diagnostics
import net.jcip.annotations.ThreadSafe
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.Reader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

@ThreadSafe
internal class ClusterNodeCounter {

    private val counter: MutableMap<String, AtomicInteger> = ConcurrentHashMap()

    fun count(node: Supplier<String>, diagnostics: Diagnostics) {
        val nodeId = try {
            node.get()
        } catch (exception: Exception) {
            diagnostics.diagnose(exception)
            "unknown"
        }
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

    fun parse(reader: Reader): Map<String, Int> {
        return CSVParser(reader, CSVFormat.DEFAULT)
            .map { record -> record.toList() }
            .associate { fields ->
                val nodeId = fields[0]
                val count = fields[1].toInt()
                nodeId to count
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
