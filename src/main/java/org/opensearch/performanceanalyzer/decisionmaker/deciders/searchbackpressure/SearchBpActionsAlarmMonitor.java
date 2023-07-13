/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure;


import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.AlarmMonitor;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.BucketizedSlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.BucketizedSlidingWindowConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;

public class SearchBpActionsAlarmMonitor implements AlarmMonitor {
    private static final Logger LOG = LogManager.getLogger(SearchBpActionsAlarmMonitor.class);
    /* Current design only uses hour monitor to evaluate the health of the searchbackpressure service
     * if there are more than 30 bad units in one hour, then the alarm shows a Unhealthy Signal
     * TODO: Remove 2 for testing, replace with 30
     */
    private static final int DEFAULT_HOUR_BREACH_THRESHOLD = 2;
    private static final int DEFAULT_BUCKET_WINDOW_SIZE = 1;
    // private static final int DEFAULT_DAY_BREACH_THRESHOLD = 1;
    // private static final int DEFAULT_WEEK_BREACH_THRESHOLD = 1;
    private static final String HOUR_PREFIX = "hour-";
    // private static final String DAY_PREFIX = "day-";
    // private static final String WEEK_PREFIX = "week-";

    public static final int HOUR_MONITOR_BUCKET_WINDOW_MINUTES = 5;
    // public static final int DAY_MONITOR_BUCKET_WINDOW_MINUTES = 30;
    // public static final int WEEK_MONITOR_BUCKET_WINDOW_MINUTES = 86400;

    private BucketizedSlidingWindow hourMonitor;
    // private BucketizedSlidingWindow dayMonitor;
    // private BucketizedSlidingWindow weekMonitor;

    private int hourBreachThreshold;
    // private int dayBreachThreshold;
    // private int weekBreachThreshold;

    private boolean alarmHealthy = true;

    @Override
    public boolean isHealthy() {
        evaluateAlarm();
        return alarmHealthy;
    }

    public SearchBpActionsAlarmMonitor(
            int hourBreachThreshold,
            @Nullable Path persistencePath,
            @Nullable BucketizedSlidingWindowConfig hourMonitorConfig) {
        Path hourMonitorPath = null;
        if (persistencePath != null) {
            Path persistenceBase = persistencePath.getParent();
            Path persistenceFile = persistencePath.getFileName();
            if (persistenceBase != null && persistenceFile != null) {
                hourMonitorPath =
                        Paths.get(
                                persistenceBase.toString(),
                                HOUR_PREFIX + persistenceFile.toString());
                // weekMonitorPath =
                //         Paths.get(
                //                 persistenceBase.toString(),
                //                 WEEK_PREFIX + persistenceFile.toString());
            }
        }
        // initialize hour Monitor
        if (hourMonitorConfig == null) {
            /*
             * Bucket Window Size means the number of issues can exist in a bucket
             * when you consider about the size of the BucketizedSlidingWindow, the size is the
             * number of buckets, not issues
             */
            hourMonitor =
                    new BucketizedSlidingWindow(
                            (int) TimeUnit.HOURS.toMinutes(1),
                            DEFAULT_BUCKET_WINDOW_SIZE,
                            TimeUnit.MINUTES,
                            hourMonitorPath);
        } else {
            hourMonitor = new BucketizedSlidingWindow(hourMonitorConfig);
        }
        // // initialize weekMonitor
        // if (weekMonitorConfig == null) {
        //     weekMonitor = new BucketizedSlidingWindow(4, 1, TimeUnit.DAYS, weekMonitorPath);
        // } else {
        //     weekMonitor = new BucketizedSlidingWindow(weekMonitorConfig);
        // }

        this.hourBreachThreshold = hourBreachThreshold;
        // this.weekBreachThreshold = weekBreachThreshold;
    }

    public SearchBpActionsAlarmMonitor(int hourBreachThreshold, @Nullable Path persistencePath) {
        this(hourBreachThreshold, persistencePath, null);
    }

    public SearchBpActionsAlarmMonitor(int hourBreachThreshold) {
        this(hourBreachThreshold, null, null);
    }

    public SearchBpActionsAlarmMonitor(@Nullable Path persistencePath) {
        this(DEFAULT_HOUR_BREACH_THRESHOLD, persistencePath);
    }

    public SearchBpActionsAlarmMonitor() {
        this(DEFAULT_HOUR_BREACH_THRESHOLD);
    }

    @Override
    public void recordIssue(long timeStamp, double value) {
        SlidingWindowData dataPoint = new SlidingWindowData(timeStamp, value);
        LOG.info("Search Backpressure Actions Alarm is recording a new issue at {}", timeStamp);
        hourMonitor.next(dataPoint);

        // // If we've breached the hour threshold, record it as a bad day/
        // if (hourMonitor.size() >= hourBreachThreshold) {
        //     dayMonitor.next(new SlidingWindowData(dataPoint.getTimeStamp(),
        // dataPoint.getValue()));
        // }
    }

    private void evaluateAlarm() {
        if (alarmHealthy) {
            LOG.info("Alarm healthy with hourMonitor.size() = {}", hourMonitor.size());
            if (hourMonitor.size() >= hourBreachThreshold) {
                LOG.info(
                        "Search Backpressure Actions Alarm is Unhealthy because hourMonitor.size() is {}, and threshold is {}",
                        hourMonitor.size(),
                        hourBreachThreshold);
                alarmHealthy = false;
            }
        } else {
            LOG.info("Alarm not healthy");
            if (hourMonitor.size() == 0) {
                LOG.info("SearchBackpressure Hour Monitor is healthy for zero capacity");
                alarmHealthy = true;
            }
        }
    }

    public int getHourBreachThreshold() {
        return hourBreachThreshold;
    }

    // public int getDayBreachThreshold() {
    //     return dayBreachThreshold;
    // }

    // public int getWeekBreachThreshold() {
    //     return weekBreachThreshold;
    // }

    @VisibleForTesting
    BucketizedSlidingWindow getHourMonitor() {
        return hourMonitor;
    }

    // @VisibleForTesting
    // BucketizedSlidingWindow getDayMonitor() {
    //     return dayMonitor;
    // }

    // @VisibleForTesting
    // BucketizedSlidingWindow getWeekMonitor() {
    //     return weekMonitor;
    // }

    @VisibleForTesting
    void setAlarmHealth(boolean isHealthy) {
        this.alarmHealthy = isHealthy;
    }
}
