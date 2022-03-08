/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.listener;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.core.Stats;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.stats.listeners.IListener;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public class MisbehavingGraphOperateMethodListener implements IListener {
    private static final Logger LOG =
            LogManager.getLogger(MisbehavingGraphOperateMethodListener.class);
    /**
     * A map to keep track of the graohNodeName and the number of times it threw an exception in the
     * {@code operate()} method.
     */
    Map<String, AtomicInteger> map;

    public static final int TOLERANCE_LIMIT = 1;

    public MisbehavingGraphOperateMethodListener() {
        map = new ConcurrentHashMap<>();
    }

    @Override
    public Set<MeasurementSet> getMeasurementsListenedTo() {
        return new HashSet<>(Arrays.asList(ExceptionsAndErrors.EXCEPTION_IN_OPERATE));
    }

    @Override
    public void onOccurrence(MeasurementSet measurementSet, Number value, String key) {
        if (!key.isEmpty()) {
            AtomicInteger count = map.putIfAbsent(key, new AtomicInteger(1));
            int newCount = 1;
            if (count != null) {
                newCount = count.incrementAndGet();
            }
            if (newCount > TOLERANCE_LIMIT) {
                if (Stats.getInstance().addToMutedGraphNodes(key)) {
                    LOG.warn(
                            "Node {} got muted for throwing one or more of '{}' more than {} times.",
                            key,
                            getMeasurementsListenedTo(),
                            TOLERANCE_LIMIT);
                }
            }
        }
    }
}
