package com.atlassian.performance.tools.virtualusers.collections

internal class CircularIterator<T>(private val delegate: Iterable<T>) : Iterator<T> {

    private var iterator: Iterator<T> = delegate.iterator()

    override fun next(): T {
        if (!iterator.hasNext()) {
            iterator = delegate.iterator()
        }
        return iterator.next()
    }

    override fun hasNext(): Boolean = delegate.any()
}