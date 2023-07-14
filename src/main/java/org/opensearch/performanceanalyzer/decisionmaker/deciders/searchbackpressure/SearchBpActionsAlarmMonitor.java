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
    /* Current design uses hour monitor to evaluate the health of the searchbackpressure service
     * if there are more than 30 bad resournce units in one hour, then the alarm shows a Unhealthy Signal
     */

    // TODO: Remove 3 for testing, replace with 30
    private static final int DEFAULT_HOUR_BREACH_THRESHOLD = 3;
    private static final int DEFAULT_BUCKET_WINDOW_SIZE = 1;
    private static final String HOUR_PREFIX = "hour-";

    public static final int HOUR_MONITOR_BUCKET_WINDOW_MINUTES = 5;

    private BucketizedSlidingWindow hourMonitor;
    private int hourBreachThreshold;

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
            }
        }
        // initialize hourly alarm monitor
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

        this.hourBreachThreshold = hourBreachThreshold;
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
    }

    private void evaluateAlarm() {
        if (alarmHealthy) {
            if (hourMonitor.size() >= hourBreachThreshold) {
                LOG.info(
                        "Search Backpressure Actions Alarm is Unhealthy because hourMonitor.size() is {}, and threshold is {}",
                        hourMonitor.size(),
                        hourBreachThreshold);
                alarmHealthy = false;
            }
        } else {
            if (hourMonitor.size() == 0) {
                LOG.info("SearchBackpressure Hour Monitor is now healthy for zero capacity");
                alarmHealthy = true;
            }
        }
    }

    public int getHourBreachThreshold() {
        return hourBreachThreshold;
    }

    @VisibleForTesting
    BucketizedSlidingWindow getHourMonitor() {
        return hourMonitor;
    }

    @VisibleForTesting
    void setAlarmHealth(boolean isHealthy) {
        this.alarmHealthy = isHealthy;
    }
}
