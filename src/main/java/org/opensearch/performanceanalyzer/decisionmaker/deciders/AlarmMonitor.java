/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;

/**
 * AlarmMonitor evaluates and maintains the state of an alarm.
 *
 * <p>An alarm can either be healthy or unhealthy.
 */
public interface AlarmMonitor {

    /**
     * Invoked whenever an issue needs to be recorded with the monitor
     *
     * @param timeStamp Issue timestamp in millis
     * @param value Issues can be recorded with an intensity value
     */
    void recordIssue(long timeStamp, double value);

    default void recordIssue() {
        recordIssue(System.currentTimeMillis(), 1);
    }

    /**
     * State of the alarm
     *
     * @return true if alarm is in healthy state, false otherwise
     */
    boolean isHealthy();
}
