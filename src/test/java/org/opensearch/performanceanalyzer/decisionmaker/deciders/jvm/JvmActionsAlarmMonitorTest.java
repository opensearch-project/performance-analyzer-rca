/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.AlarmMonitor;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;

public class JvmActionsAlarmMonitorTest {

    @Test
    public void testInit() {
        AlarmMonitor monitor = new JvmActionsAlarmMonitor();
        assertTrue(monitor.isHealthy());
    }

    @Test
    public void testFlipToUnhealthy() {
        AlarmMonitor monitor = new JvmActionsAlarmMonitor();
        JvmActionsAlarmMonitor jvmMonitor = (JvmActionsAlarmMonitor) monitor;
        long startTimeInMins = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());

        // Record issues and breach day threshold
        long currTime;
        long thresholdBreachTS = startTimeInMins + 30 * jvmMonitor.getDayBreachThreshold();
        for (currTime = startTimeInMins; currTime < thresholdBreachTS; currTime++) {
            monitor.recordIssue(TimeUnit.MINUTES.toMillis(currTime), 1);
        }
        Assert.assertEquals(jvmMonitor.getDayBreachThreshold(), jvmMonitor.getDayMonitor().size());
        Assert.assertEquals(1, jvmMonitor.getWeekMonitor().size());
        assertTrue(monitor.isHealthy());

        // More issues within the same day do not add to week monitor
        for (currTime = thresholdBreachTS; currTime < thresholdBreachTS + 120; currTime++) {
            monitor.recordIssue(TimeUnit.MINUTES.toMillis(currTime), 1);
        }
        Assert.assertEquals(1, jvmMonitor.getWeekMonitor().size());
        assertTrue(jvmMonitor.getDayMonitor().size() > 5);
        assertTrue(monitor.isHealthy());

        // Add issues after 2 days
        currTime += TimeUnit.DAYS.toMinutes(2);
        for (int i = 0; i < jvmMonitor.getDayBreachThreshold(); i++) {
            currTime += 30;
            monitor.recordIssue(TimeUnit.MINUTES.toMillis(currTime), 1);
        }
        Assert.assertEquals(jvmMonitor.getDayBreachThreshold(), jvmMonitor.getDayMonitor().size());
        Assert.assertEquals(2, jvmMonitor.getWeekMonitor().size());
        assertFalse(monitor.isHealthy());
    }

    @Test
    public void testFlipToHealthy() {
        AlarmMonitor monitor = new JvmActionsAlarmMonitor();
        JvmActionsAlarmMonitor jvmMonitor = (JvmActionsAlarmMonitor) monitor;
        jvmMonitor.setAlarmHealth(false);

        // Since both monitors are empty, the alarm evaluates to healthy
        assertTrue(monitor.isHealthy());

        // Only day issues present
        jvmMonitor.setAlarmHealth(false);
        long currTime = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
        monitor.recordIssue(TimeUnit.MINUTES.toMillis(currTime), 1);
        Assert.assertEquals(1, jvmMonitor.getDayMonitor().size());
        Assert.assertEquals(0, jvmMonitor.getWeekMonitor().size());
        assertFalse(monitor.isHealthy());
    }

    @Test
    public void testFlipToHealthyWithWeekMonitorFlagged() {
        AlarmMonitor monitor = new JvmActionsAlarmMonitor();
        JvmActionsAlarmMonitor jvmMonitor = (JvmActionsAlarmMonitor) monitor;
        jvmMonitor.setAlarmHealth(false);

        jvmMonitor.getWeekMonitor().next(new SlidingWindowData(System.currentTimeMillis(), 1));
        Assert.assertEquals(0, jvmMonitor.getDayMonitor().size());
        Assert.assertEquals(1, jvmMonitor.getWeekMonitor().size());
        assertFalse(monitor.isHealthy());
    }
}
