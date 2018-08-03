package com.atlassian.performance.tools.virtualusers.logs;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.FileAppender;

/**
 * The class just forwards the FileAppender builder. The builder cannot be used in kotlin directly
 * due to https://youtrack.jetbrains.com/issue/KT-17186.
 */
public class FileAppenderBuilderWrapper {
    public FileAppender create(
            String name,
            String fileName,
            boolean isAppendable,
            Layout<String> layout
    ) {
        return FileAppender.newBuilder()
                .withName(name)
                .withLayout(layout)
                .withFileName(fileName)
                .withAppend(isAppendable)
                .build();
    }
}