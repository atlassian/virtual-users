package com.atlassian.performance.tools.virtualusers.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Test
import java.net.URI

class VirtualUserOptionsTest {
    @Test
    fun shouldReturnSamePathIfValid() {
        val options = VirtualUserOptions(jiraAddress = URI("http://localhost:8080/"))

        assertThat(options.toCliArgs()).contains("http://localhost:8080/")
    }

    @Test
    fun shouldAppendPathIfMissing() {
        val options = VirtualUserOptions(jiraAddress = URI("http://localhost:8080"))

        assertThat(options.toCliArgs()).contains("http://localhost:8080/")
    }

    @Test
    fun shouldThrowOnInvalidUri() {
        val thrown = catchThrowable {
            VirtualUserOptions(jiraAddress = URI("http://localhost:8080invalid"))
        }

        assertThat(thrown).hasMessageContaining("http://localhost:8080invalid")
    }

    @Test
    fun shouldFixDanglingContextPath() {
        val options = VirtualUserOptions(jiraAddress = URI("http://localhost:8080/context-path"))

        assertThat(options.toCliArgs()).contains("http://localhost:8080/context-path/")
    }

}