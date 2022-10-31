/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.persistence;


import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerThreads;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Used;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.core.ThresholdMain;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaUtil;
import org.opensearch.performanceanalyzer.rca.net.WireHopper;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.scheduler.RCAScheduler;
import org.opensearch.performanceanalyzer.rca.spec.MetricsDBProviderTestHelper;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.HotNodeRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;
import org.opensearch.performanceanalyzer.threads.ThreadProvider;
import org.opensearch.performanceanalyzer.util.WaitFor;

@Category(GradleTaskForRca.class)
@SuppressWarnings("serial")
public class PersistFlowUnitAndSummaryTest {
    Queryable queryable;

    static class DummyYoungGenRca extends Rca<ResourceFlowUnit<HotResourceSummary>> {
        public <M extends Metric> DummyYoungGenRca(M metric) {
            super(1);
        }

        @Override
        public ResourceFlowUnit<HotResourceSummary> operate() {
            ResourceContext context = new ResourceContext(Resources.State.UNHEALTHY);
            HotResourceSummary summary =
                    new HotResourceSummary(ResourceUtil.YOUNG_GEN_PROMOTION_RATE, 400, 100, 60);
            return new ResourceFlowUnit<>(System.currentTimeMillis(), context, summary);
        }

        @Override
        public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {}
    }

    static class HotNodeRcaX extends HotNodeRca {
        public <R extends Rca<ResourceFlowUnit<HotResourceSummary>>> HotNodeRcaX(
                final int rcaPeriod, R... hotResourceRcas) {
            super(rcaPeriod, hotResourceRcas);
            this.evaluationIntervalSeconds = 1;
        }
    }

    static class HighHeapUsageClusterRcaX extends HighHeapUsageClusterRca {
        public <R extends Rca> HighHeapUsageClusterRcaX(final int rcaPeriod, final R hotNodeRca) {
            super(rcaPeriod, hotNodeRca);
            this.evaluationIntervalSeconds = 1;
        }
    }

    static class DataNodeGraph extends AnalysisGraph {

        @Override
        public void construct() {
            Metric heapUsed = new Heap_Used(5);
            addLeaf(heapUsed);
            Rca<ResourceFlowUnit<HotResourceSummary>> dummyYoungGenRca =
                    new DummyYoungGenRca(heapUsed);
            dummyYoungGenRca.addAllUpstreams(Collections.singletonList(heapUsed));
            dummyYoungGenRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS, RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

            Rca<ResourceFlowUnit<HotNodeSummary>> nodeRca = new HotNodeRcaX(1, dummyYoungGenRca);
            nodeRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS, RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);
            nodeRca.addAllUpstreams(Collections.singletonList(dummyYoungGenRca));
        }
    }

    static class ClusterManagerNodeGraph extends AnalysisGraph {

        @Override
        public void construct() {
            Metric heapUsed = new Heap_Used(5);
            heapUsed.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
            addLeaf(heapUsed);
            Rca<ResourceFlowUnit<HotResourceSummary>> dummyYoungGenRca =
                    new DummyYoungGenRca(heapUsed);
            dummyYoungGenRca.addAllUpstreams(Collections.singletonList(heapUsed));
            dummyYoungGenRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);

            Rca<ResourceFlowUnit<HotNodeSummary>> nodeRca = new HotNodeRcaX(1, dummyYoungGenRca);
            nodeRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
            nodeRca.addAllUpstreams(Collections.singletonList(dummyYoungGenRca));

            Rca<ResourceFlowUnit<HotClusterSummary>> highHeapUsageClusterRca =
                    new HighHeapUsageClusterRcaX(1, nodeRca);
            highHeapUsageClusterRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
            highHeapUsageClusterRca.addAllUpstreams(Collections.singletonList(nodeRca));
        }
    }

    @Before
    public void before() throws Exception {
        queryable = new MetricsDBProviderTestHelper(false);
    }

    private RCAScheduler startScheduler(
            RcaConf rcaConf,
            AnalysisGraph graph,
            Persistable persistable,
            Queryable queryable,
            AppContext appContext) {
        RCAScheduler scheduler =
                new RCAScheduler(
                        RcaUtil.getAnalysisGraphComponents(graph),
                        queryable,
                        rcaConf,
                        new ThresholdMain(
                                Paths.get(RcaConsts.TEST_CONFIG_PATH, "thresholds").toString(),
                                rcaConf),
                        persistable,
                        new WireHopper(null, null, null, null, null, appContext),
                        appContext);
        ThreadProvider threadProvider = new ThreadProvider();
        Thread rcaSchedulerThread =
                threadProvider.createThreadForRunnable(
                        scheduler::start, PerformanceAnalyzerThreads.RCA_SCHEDULER);
        rcaSchedulerThread.start();
        return scheduler;
    }

    private AppContext createAppContextWithDataNodes(
            String nodeName, AllMetrics.NodeRole role, boolean isClusterManager) {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();

        ClusterDetailsEventProcessor.NodeDetails node1 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        role, nodeName, "127.0.0.0", isClusterManager);
        nodes.add(node1);

        clusterDetailsEventProcessor.setNodesDetails(nodes);

        AppContext appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
        return appContext;
    }

    /**
     * Add testPersistSummaryOnDataNode() and testPersistSummaryOnClusterManagerNode() into a single
     * UT This will force both tests to run in sequential and can avoid access contention to the
     * same db file.
     *
     * @throws Exception SQL exception
     */
    @Test
    public void testPersisSummary() throws Exception {
        RcaConf rcaConf = new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());
        RcaConf clusterManagerRcaConf =
                new RcaConf(
                        Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca_elected_cluster_manager.conf")
                                .toString());
        Persistable persistable = PersistenceFactory.create(rcaConf);
        testPersistSummaryOnDataNode(rcaConf, persistable);
        persistable = PersistenceFactory.create(rcaConf);
        testPersistSummaryOnClusterManagerNode(clusterManagerRcaConf, persistable);
    }

    private void testPersistSummaryOnDataNode(RcaConf rcaConf, Persistable persistable)
            throws Exception {
        AppContext appContext =
                createAppContextWithDataNodes("node1", AllMetrics.NodeRole.DATA, false);

        AnalysisGraph graph = new DataNodeGraph();
        RCAScheduler scheduler =
                startScheduler(rcaConf, graph, persistable, this.queryable, appContext);
        // Wait at most 1 minute for the persisted data to show up with the correct contents
        WaitFor.waitFor(
                () -> {
                    String readTableStr = persistable.read();
                    System.out.println(readTableStr);
                    if (readTableStr != null) {
                        // HighHeapUsageClusterRcaX is a cluster level RCA so it should not be
                        // scheduled and persisted on
                        // data node.
                        return readTableStr.contains("HotResourceSummary")
                                && readTableStr.contains("DummyYoungGenRca")
                                && readTableStr.contains("HotNodeSummary")
                                && readTableStr.contains("HotNodeRcaX");
                    }
                    return false;
                },
                1,
                TimeUnit.MINUTES);
        scheduler.shutdown();
    }

    private void testPersistSummaryOnClusterManagerNode(RcaConf rcaConf, Persistable persistable)
            throws Exception {
        AppContext appContext =
                createAppContextWithDataNodes("node1", AllMetrics.NodeRole.DATA, true);
        AnalysisGraph graph = new ClusterManagerNodeGraph();
        RCAScheduler scheduler =
                startScheduler(rcaConf, graph, persistable, this.queryable, appContext);
        // Wait at most 1 minute for the persisted data to show up with the correct contents
        WaitFor.waitFor(
                () -> {
                    String readTableStr = persistable.read();
                    if (readTableStr != null) {
                        return readTableStr.contains("HotResourceSummary")
                                && readTableStr.contains("DummyYoungGenRca")
                                && readTableStr.contains("HotNodeSummary")
                                && readTableStr.contains("HotNodeRcaX")
                                && readTableStr.contains("HighHeapUsageClusterRcaX");
                    }
                    return false;
                },
                1,
                TimeUnit.MINUTES);
        scheduler.shutdown();
    }

    @After
    public void cleanup() throws Exception {
        if (queryable != null) {
            queryable.getMetricsDB().close();
            queryable.getMetricsDB().deleteOnDiskFile();
        }
    }
}
