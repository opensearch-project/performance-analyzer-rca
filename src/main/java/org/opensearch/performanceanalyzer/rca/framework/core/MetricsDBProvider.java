/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;


import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;

public class MetricsDBProvider implements Queryable {
    private static final Logger LOG = LogManager.getLogger(MetricsDBProvider.class);

    @Override
    public MetricsDB getMetricsDB() throws Exception {
        ReaderMetricsProcessor processor = ReaderMetricsProcessor.getInstance();
        if (processor == null) {
            LOG.error("RCA: ReaderMetricsProcessor not initialized");
            throw new Exception("ReaderMetricsProcessor not initialized");
        }
        Map.Entry<Long, MetricsDB> dbEntry = processor.getMetricsDB();
        if (dbEntry == null) {
            LOG.error("RCA: MetricsDB not initialized");
            throw new Exception("Metrics DB not initialized");
        }
        return dbEntry.getValue();
    }

    /**
     * This queries the MetricsDB to get all the data for the given metric.
     *
     * <p>If we query for a metric that does not exist then {@code queryMetrics()} with throw {@code
     * exception}, which is not handled here. The caller might handle if it wants to.
     *
     * @param db The MetricsDB file to query
     * @param metricName The table for the metric that will be queried.
     * @return Returns the metrics data in a tabular form.
     */
    @Override
    public Result<Record> queryMetrics(MetricsDB db, String metricName) {
        return db.queryMetric(metricName);
    }

    @Override
    public Result<Record> queryMetrics(
            final MetricsDB db,
            final String metricName,
            final String dimension,
            final String aggregation)
            throws Exception {
        Result<Record> queryResult =
                db.queryMetric(
                        Collections.singletonList(metricName),
                        Collections.singletonList(aggregation),
                        Collections.singletonList(dimension));
        return queryResult;
    }

    @Override
    public long getDBTimestamp(MetricsDB db) {
        return 0;
    }
}
