package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import com.atlassian.performance.tools.virtualusers.api.TemporalRate
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.browsers.GoogleChrome
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior.Builder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds

class VirtualUserBehaviorTest {

    @Test
    fun shouldCopyWithLoad() {
        val originalLoad = VirtualUserLoad.Builder()
            .virtualUsers(1)
            .hold(ofSeconds(3))
            .ramp(ofSeconds(4))
            .flat(ofMinutes(10))
            .maxOverallLoad(TemporalRate(2.0, ofSeconds(2)))
            .build()
        val original = Builder(JiraSoftwareScenario::class.java)
            .load(originalLoad)
            .seed(7381)
            .diagnosticsLimit(4)
            .browser(GoogleChrome::class.java)
            .build()
        val newLoad = VirtualUserLoad.Builder()
            .virtualUsers(2)
            .hold(ofSeconds(6))
            .ramp(ofSeconds(8))
            .flat(ofSeconds(20))
            .build()

        val copied = Builder(original)
            .load(newLoad)
            .build()

        val comparator = compareBy<VirtualUserLoad>(
            { it.virtualUsers },
            { it.hold },
            { it.ramp },
            { it.flat },
            { it.maxOverallLoad }
        )
        assertThat(copied.load)
            .usingComparator(comparator)
            .isEqualTo(newLoad)
        assertThat(original.load)
            .usingComparator(comparator)
            .isEqualTo(originalLoad)
    }
}
