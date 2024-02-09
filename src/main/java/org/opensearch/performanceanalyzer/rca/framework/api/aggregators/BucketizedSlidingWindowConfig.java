/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class BucketizedSlidingWindowConfig {
    private int slidingWindowSizeMinutes;
    private int bucketSizeMinutes;

    private TimeUnit timeUnit;
    private Path persistencePath;

    public BucketizedSlidingWindowConfig(
            int slidingWindowSizeMinutes,
            int bucketSizeMinutes,
            TimeUnit timeUnit,
            @Nullable Path persistencePath) {
        this.slidingWindowSizeMinutes = slidingWindowSizeMinutes;
        this.bucketSizeMinutes = bucketSizeMinutes;
        this.timeUnit = timeUnit;
        this.persistencePath = persistencePath;
    }

    public int getSlidingWindowSizeMinutes() {
        return slidingWindowSizeMinutes;
    }

    public void setSlidingWindowSizeMinutes(int slidingWindowSizeMinutes) {
        this.slidingWindowSizeMinutes = slidingWindowSizeMinutes;
    }

    public int getBucketSizeMinutes() {
        return bucketSizeMinutes;
    }

    public void setBucketSizeMinutes(int bucketSizeMinutes) {
        this.bucketSizeMinutes = bucketSizeMinutes;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Path getPersistencePath() {
        return persistencePath;
    }

    public void setPersistencePath(Path persistencePath) {
        this.persistencePath = persistencePath;
    }
}
