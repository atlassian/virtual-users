package com.atlassian.performance.tools.virtualusers.collections

import org.junit.Test

class CircularIteratorTest {

    @Test
    fun elementsMatchWithin3loops() {
        val list = arrayListOf(1, 2, 3, 4)
        val circularIterator = CircularIterator(list)

        for (element in (list + list + list)) {
            assert(element == circularIterator.next())
        }
    }

    @Test
    fun notEndBefore100loops() {
        val list = arrayListOf(1, 2, 3, 4)
        val circularIterator = CircularIterator(list)

        var iterCount = 0
        for (element in circularIterator) {
            if (iterCount >= 100 * list.size) {
                break
            }
            iterCount++
        }

        assert(iterCount == 100 * list.size)
    }

    @Test
    fun loop100TimesOver1Element() {
        val list = arrayListOf(1)
        val circularIterator = CircularIterator(list)

        var iterCount = 0
        for (element in circularIterator) {
            assert(element == 1)

            if (iterCount >= 100) {
                break
            }
            iterCount++
        }
        assert(iterCount == 100)
    }

    @Test
    fun emptyIfDelegateIsEmpty() {
        val list = emptyList<Int>()
        val circularIterator = CircularIterator(list)

        for (element in circularIterator) {
            assert(false)
        }
    }
}