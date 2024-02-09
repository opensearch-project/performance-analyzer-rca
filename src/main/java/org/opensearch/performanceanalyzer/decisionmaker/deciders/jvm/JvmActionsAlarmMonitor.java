/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.AlarmMonitor;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.BucketizedSlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.BucketizedSlidingWindowConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;

public class JvmActionsAlarmMonitor implements AlarmMonitor {

    private static final int DEFAULT_DAY_BREACH_THRESHOLD = 5;
    private static final int DEFAULT_WEEK_BREACH_THRESHOLD = 2;
    private static final String DAY_PREFIX = "day-";
    private static final String WEEK_PREFIX = "week-";

    public static final int DAY_MONITOR_BUCKET_WINDOW_MINUTES = 30;
    public static final int WEEK_MONITOR_BUCKET_WINDOW_MINUTES = 86400;

    private BucketizedSlidingWindow dayMonitor;
    private BucketizedSlidingWindow weekMonitor;
    private int dayBreachThreshold;
    private int weekBreachThreshold;
    private boolean alarmHealthy = true;

    public JvmActionsAlarmMonitor(
            int dayBreachThreshold,
            int weekBreachThreshold,
            @Nullable Path persistencePath,
            @Nullable BucketizedSlidingWindowConfig dayMonitorConfig,
            @Nullable BucketizedSlidingWindowConfig weekMonitorConfig) {
        Path dayMonitorPath = null;
        Path weekMonitorPath = null;
        if (persistencePath != null) {
            Path persistenceBase = persistencePath.getParent();
            Path persistenceFile = persistencePath.getFileName();
            if (persistenceBase != null && persistenceFile != null) {
                dayMonitorPath =
                        Paths.get(
                                persistenceBase.toString(),
                                DAY_PREFIX + persistenceFile.toString());
                weekMonitorPath =
                        Paths.get(
                                persistenceBase.toString(),
                                WEEK_PREFIX + persistenceFile.toString());
            }
        }
        // initialize dayMonitor
        if (dayMonitorConfig == null) {
            dayMonitor =
                    new BucketizedSlidingWindow(
                            (int) TimeUnit.DAYS.toMinutes(1), 30, TimeUnit.MINUTES, dayMonitorPath);
        } else {
            dayMonitor = new BucketizedSlidingWindow(dayMonitorConfig);
        }
        // initialize weekMonitor
        if (weekMonitorConfig == null) {
            weekMonitor = new BucketizedSlidingWindow(4, 1, TimeUnit.DAYS, weekMonitorPath);
        } else {
            weekMonitor = new BucketizedSlidingWindow(weekMonitorConfig);
        }
        this.dayBreachThreshold = dayBreachThreshold;
        this.weekBreachThreshold = weekBreachThreshold;
    }

    public JvmActionsAlarmMonitor(
            int dayBreachThreshold, int weekBreachThreshold, @Nullable Path persistencePath) {
        this(dayBreachThreshold, weekBreachThreshold, persistencePath, null, null);
    }

    public JvmActionsAlarmMonitor(int dayBreachThreshold, int weekBreachThreshold) {
        this(dayBreachThreshold, weekBreachThreshold, null, null, null);
    }

    public JvmActionsAlarmMonitor(@Nullable Path persistencePath) {
        this(DEFAULT_DAY_BREACH_THRESHOLD, DEFAULT_WEEK_BREACH_THRESHOLD, persistencePath);
    }

    public JvmActionsAlarmMonitor() {
        this(DEFAULT_DAY_BREACH_THRESHOLD, DEFAULT_WEEK_BREACH_THRESHOLD);
    }

    @Override
    public void recordIssue(long timeStamp, double value) {
        SlidingWindowData dataPoint = new SlidingWindowData(timeStamp, value);
        dayMonitor.next(dataPoint);
        // If we've breached the day threshold, record it as a bad day this week.
        if (dayMonitor.size() >= dayBreachThreshold) {
            weekMonitor.next(new SlidingWindowData(dataPoint.getTimeStamp(), dataPoint.getValue()));
        }
    }

    private void evaluateAlarm() {
        if (alarmHealthy) {
            if (weekMonitor.size() >= weekBreachThreshold) {
                alarmHealthy = false;
            }
        } else {
            if (dayMonitor.size() == 0 && weekMonitor.size() == 0) {
                alarmHealthy = true;
            }
        }
    }

    @Override
    public boolean isHealthy() {
        evaluateAlarm();
        return alarmHealthy;
    }

    public int getDayBreachThreshold() {
        return dayBreachThreshold;
    }

    public int getWeekBreachThreshold() {
        return weekBreachThreshold;
    }

    @VisibleForTesting
    BucketizedSlidingWindow getDayMonitor() {
        return dayMonitor;
    }

    @VisibleForTesting
    BucketizedSlidingWindow getWeekMonitor() {
        return weekMonitor;
    }

    @VisibleForTesting
    void setAlarmHealth(boolean isHealthy) {
        this.alarmHealthy = isHealthy;
    }
}
