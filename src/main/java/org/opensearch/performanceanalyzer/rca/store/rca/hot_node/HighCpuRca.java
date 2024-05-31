/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;

import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;

/**
 * This RCA can be used to calculate total cpu usage(combines all physical cores) and give a list of
 * top operations consuming most of cpu resource to downstream vertices
 */
public class HighCpuRca extends GenericResourceRca {
    private static final double CPU_USAGE_THRESHOLD = 0.7;

    public <M extends Metric> HighCpuRca(final int rcaPeriod, final M cpuUsageGroupByOperation) {
        super(rcaPeriod, ResourceUtil.CPU_USAGE, 0, cpuUsageGroupByOperation);
        int cores = Runtime.getRuntime().availableProcessors();
        this.setThreshold(CPU_USAGE_THRESHOLD * (double) cores);
    }
}
