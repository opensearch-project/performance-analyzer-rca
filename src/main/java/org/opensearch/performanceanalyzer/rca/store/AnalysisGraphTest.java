/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store;


import org.opensearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_AllocRate;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Paging_MajfltRate;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Sched_Waittime;

public class AnalysisGraphTest extends AnalysisGraph {

    @Override
    public void construct() {
        Metric cpuUtilization = new CPU_Utilization(5);
        Metric heapUsed = new Sched_Waittime(5);
        Metric pageMaj = new Paging_MajfltRate(5);
        Metric heapAlloc = new Heap_AllocRate(5);

        addLeaf(cpuUtilization);
        addLeaf(heapUsed);
        addLeaf(pageMaj);
        addLeaf(heapAlloc);

        System.out.println(this.getClass().getName() + " graph constructed..");
    }
}
