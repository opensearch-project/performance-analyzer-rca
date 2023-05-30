/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api;


import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.core.LeafNode;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

public abstract class Metric extends LeafNode<MetricFlowUnit> {
    static final String[] metricList;

    static {
        AllMetrics.OSMetrics[] osMetrics = AllMetrics.OSMetrics.values();
        metricList = new String[osMetrics.length];
        for (int i = 0; i < osMetrics.length; ++i) {
            metricList[i] = osMetrics[i].name();
        }
    }

    private String name;
    private static final Logger LOG = LogManager.getLogger(Metric.class);

    public Metric(String name, long evaluationIntervalSeconds) {
        super(0, evaluationIntervalSeconds);
        this.name = name.isEmpty() ? this.getClass().getSimpleName() : name;
    }

    @Override
    public String name() {
        return name;
    }

    public MetricFlowUnit gather(Queryable queryable) {
        LOG.debug("Trying to gather metrics for {}", name);
        MetricsDB db;
        try {
            db = queryable.getMetricsDB();
        } catch (Exception e) {
            ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_GATHER, name(), 1);
            // TODO: Emit log/stats that gathering failed.
            LOG.error("RCA: Caught an exception while getting the DB", e);
            return MetricFlowUnit.generic();
        }
        try {
            Result<Record> result = queryable.queryMetrics(db, name);
            return new MetricFlowUnit(queryable.getDBTimestamp(db), result);
        } catch (DataAccessException dex) {
            // This can happen if the RCA started querying for metrics before the Reader obtained
            // them.
            // This is not an error.
            // And node stats metrics can be enabled/disabled on writer side so we might end up
            // being here
            // if RCA is trying to read node stats which are not enabled yet.
            LOG.warn("Looking for metric {}, when it does not exist.", name);
        } catch (Exception e) {
            ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_GATHER, name(), 1);
            LOG.error("Metric exception:", e);
        }
        return MetricFlowUnit.generic();
    }

    public void generateFlowUnitListFromLocal(FlowUnitOperationArgWrapper args) {
        long startTime = System.currentTimeMillis();
        MetricFlowUnit mfu = gather(args.getQueryable());
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.METRIC_GATHER_CALL, this.name(), duration);
        setFlowUnits(Collections.singletonList(mfu));
    }

    /**
     * Persists the given flow unit.
     *
     * @param args The arg wrapper.
     */
    @Override
    public void persistFlowUnit(FlowUnitOperationArgWrapper args) {}

    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        LOG.error("we are not supposed to read metric flowunit from wire.");
    }

    /**
     * This method specifies what needs to be done when the current node is muted for throwing
     * exceptions.
     */
    @Override
    public void handleNodeMuted() {
        setLocalFlowUnit(MetricFlowUnit.generic());
    }
}
