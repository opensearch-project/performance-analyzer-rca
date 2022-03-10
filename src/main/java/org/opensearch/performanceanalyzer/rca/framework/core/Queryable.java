/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;


import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;

public interface Queryable {

    MetricsDB getMetricsDB() throws Exception;

    Result<Record> queryMetrics(MetricsDB db, String metricName);

    Result<Record> queryMetrics(
            MetricsDB db, String metricName, String dimension, String aggregation) throws Exception;

    long getDBTimestamp(MetricsDB db);
}
