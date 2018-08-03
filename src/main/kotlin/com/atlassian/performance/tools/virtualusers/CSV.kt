package com.atlassian.performance.tools.virtualusers

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

class CSV(
    private val csvSource: ClassLoader,
    private val resource: String
) {
    fun getParser(): CSVParser {
        return CSVFormat
            .DEFAULT
            .withEscape('\\')
            .parse(
                csvSource.getResourceAsStream(resource).bufferedReader()
            )
    }
}