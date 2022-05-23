/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;


import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hot_node.ThreadMetricsRca.ThreadMetric;
import org.opensearch.performanceanalyzer.rca.store.rca.hot_node.ThreadMetricsRca.ThreadMetricsSlidingWindow;

@Category(GradleTaskForRca.class)
public class ThreadMetricsSlidingWindowTest {

    @Test
    public void testWindow() {
        ThreadMetricsSlidingWindow window = new ThreadMetricsSlidingWindow();
        Assert.assertEquals(window.getMaxSum(), 0d, 0d);
        Assert.assertEquals(window.getCountExceedingThreshold(5d), 0d, 0);
        long currentTimeMillis = 100L;
        window.next(
                currentTimeMillis,
                Arrays.asList(
                        new ThreadMetric("t1", 10d, currentTimeMillis, "operation"),
                        new ThreadMetric("t2", 15d, currentTimeMillis, "operation")));
        Assert.assertEquals(window.getMaxSum(), 15d, 0d);
        Assert.assertEquals(window.getCountExceedingThreshold(5d), 2, 0);
        Assert.assertEquals(window.getCountExceedingThreshold(10d), 1, 0);
        currentTimeMillis = 30100L;
        window.next(
                currentTimeMillis,
                Arrays.asList(
                        new ThreadMetric("t1", 10d, currentTimeMillis, "operation"),
                        new ThreadMetric("t2", 15d, currentTimeMillis, "operation"),
                        new ThreadMetric("t3", 4d, currentTimeMillis, "operation")));
        Assert.assertEquals(window.getMaxSum(), 30d, 0d);
        Assert.assertEquals(window.getCountExceedingThreshold(5d), 2, 0);
        Assert.assertEquals(window.getCountExceedingThreshold(10d), 2, 0);
        currentTimeMillis = 40100L;
        window.next(currentTimeMillis, Collections.emptyList());
        Assert.assertEquals(window.getMaxSum(), 30d, 0d);
        Assert.assertEquals(window.getCountExceedingThreshold(5d), 2, 0);
        Assert.assertEquals(window.getCountExceedingThreshold(10d), 2, 0);
        currentTimeMillis = 61101L;
        window.next(currentTimeMillis, Collections.emptyList());
        Assert.assertEquals(window.getMaxSum(), 15d, 0d);
        Assert.assertEquals(window.getCountExceedingThreshold(5d), 2, 0);
        Assert.assertEquals(window.getCountExceedingThreshold(10d), 1, 0);
        currentTimeMillis = 111101L;
        window.next(currentTimeMillis, Collections.emptyList());
        Assert.assertEquals(window.getMaxSum(), 0, 0d);
        Assert.assertEquals(window.getCountExceedingThreshold(5d), 0, 0);
        Assert.assertEquals(window.getCountExceedingThreshold(10d), 0, 0);
    }
}
