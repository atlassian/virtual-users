package com.atlassian.performance.tools.virtualusers

import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Duration.ofSeconds

class VirtualUserLoadTest {

    private val load = VirtualUserLoad(
            virtualUsers = 12,
            hold = ofSeconds(10),
            ramp = ofSeconds(16),
            flat = ofSeconds(55)
    )

    @Test
    fun shouldSubdivideIntoFour() {
        val subLoads = load.subdivide(4)

        assertThat(
                subLoads,
                hasItems(
                        VirtualUserLoad(
                                virtualUsers = 3,
                                hold = ofSeconds(10),
                                ramp = ofSeconds(4),
                                flat = ofSeconds(67)
                        ),
                        VirtualUserLoad(
                                virtualUsers = 3,
                                hold = ofSeconds(14),
                                ramp = ofSeconds(4),
                                flat = ofSeconds(63)
                        ),
                        VirtualUserLoad(
                                virtualUsers = 3,
                                hold = ofSeconds(18),
                                ramp = ofSeconds(4),
                                flat = ofSeconds(59)
                        ),
                        VirtualUserLoad(
                                virtualUsers = 3,
                                hold = ofSeconds(22),
                                ramp = ofSeconds(4),
                                flat = ofSeconds(55)
                        )
                )
        )
    }

    @Test
    fun shouldSubdivideIntoItself() {
        val subLoads = load.subdivide(1)

        assertThat(subLoads, equalTo(listOf(load)))
    }

    @Test
    fun shouldSubdivideToEqualTotals() {
        val subLoads = load.subdivide(10)

        subLoads.forEach { subLoad ->
            assertThat(subLoad.total, equalTo(load.total))
        }
    }

    @Test
    fun shouldSubdivideTheGivenNumberOfTimes() {
        val parts = 11

        val subLoads = load.subdivide(parts)

        assertThat(subLoads.size, equalTo(parts))
    }

    @Test
    fun shouldAvoidEmptyParts() {
        val exception: Exception? = try {
            load.subdivide(300)
            null
        } catch (e: Exception) {
            e
        }

        assertThat(exception, notNullValue())
        assertThat(exception!!.message, equalTo("12 virtual users are not enough to subdivide into 300 parts"))
    }
}
