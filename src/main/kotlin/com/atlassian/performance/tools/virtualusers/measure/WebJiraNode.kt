package com.atlassian.performance.tools.virtualusers.measure

import com.atlassian.performance.tools.jiraactions.api.WebJira

internal class WebJiraNode(
    private val jira: WebJira
) : ApplicationNode {
    override fun identify(): String = jira.getJiraNode()
}
