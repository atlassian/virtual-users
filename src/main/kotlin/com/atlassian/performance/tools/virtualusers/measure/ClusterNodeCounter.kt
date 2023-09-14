package com.atlassian.performance.tools.virtualusers.measure

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.Reader
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

public class ClusterNodeCounter {

    private val counter: MutableMap<String, AtomicInteger> = ConcurrentHashMap()
    private val logger: Logger = LogManager.getLogger(this::class.java)

    fun count(node: Supplier<String>) {
        val nodeId = try {
            node.get()
        } catch (exception: Exception) {
            logger.warn("Failed to identify jira node", exception)
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
