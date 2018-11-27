package com.atlassian.performance.tools.virtualusers.api.config

import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.virtualusers.api.browsers.GoogleChrome
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration

class VirtualUserBehaviorTest {

    @Test
    fun shouldCopyWithLoad() {
        val original = VirtualUserBehavior(
            scenario = JiraSoftwareScenario::class.java,
            load = VirtualUserLoad(
                virtualUsers = 1,
                hold = Duration.ofSeconds(3),
                ramp = Duration.ofSeconds(4),
                flat = Duration.ofMinutes(10)
            ),
            seed = 7381,
            diagnosticsLimit = 4,
            browser = GoogleChrome::class.java
        )

        val copied = original.withLoad(
            VirtualUserLoad(
                virtualUsers = 2,
                hold = Duration.ofSeconds(6),
                ramp = Duration.ofSeconds(8),
                flat = Duration.ofSeconds(20)
            )
        )

        val comparator = compareBy<VirtualUserLoad>(
            { it.virtualUsers },
            { it.hold },
            { it.ramp },
            { it.flat }
        )
        assertThat(copied.load)
            .usingComparator(comparator)
            .isEqualTo(
                VirtualUserLoad(
                    virtualUsers = 2,
                    hold = Duration.ofSeconds(6),
                    ramp = Duration.ofSeconds(8),
                    flat = Duration.ofSeconds(20)
                )
            )
        assertThat(original.load)
            .usingComparator(comparator)
            .isEqualTo(
                VirtualUserLoad(
                    virtualUsers = 1,
                    hold = Duration.ofSeconds(3),
                    ramp = Duration.ofSeconds(4),
                    flat = Duration.ofMinutes(10)
                )
            )
    }
}