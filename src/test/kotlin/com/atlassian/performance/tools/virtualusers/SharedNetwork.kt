package com.atlassian.performance.tools.virtualusers

import org.junit.rules.ExternalResource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.Network
import org.testcontainers.utility.ResourceReaper

internal class SharedNetwork(name: String) : ExternalResource(), Network {
    private val id: String

    init {
        id = DockerClientFactory
            .instance()
            .client()
            .listNetworksCmd()
            .withNameFilter(name)
            .exec()
            .single()
            .id
    }

    override fun getId(): String {
       return this.id
    }

    override fun close() {
        ResourceReaper.instance().removeNetworkById(id)
    }
}