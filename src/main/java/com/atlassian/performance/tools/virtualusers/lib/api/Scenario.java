package com.atlassian.performance.tools.virtualusers.lib.api;

import com.atlassian.performance.tools.jiraactions.api.action.Action;
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter;
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget;

import java.util.List;

/**
 * IT doesn't need to be thread safe. Each VU will have own copy of the scenario and own Thread
 */
public abstract class Scenario {

    /**
     * vu will use this constructor to create the scenario
     *
     * @param virtualUserTarget
     * @param meter
     */
    protected Scenario(VirtualUserTarget virtualUserTarget, ActionMeter meter) {
    }

    /**
     * The method will be called before VU starts executing actions
     */
    public void before() {

    }

    /**
     * The method will be called Once to setUp product instance
     */
    public void setup() {

    }

    public abstract List<Action> getActions();

    public void cleanUp() {

    }
}
