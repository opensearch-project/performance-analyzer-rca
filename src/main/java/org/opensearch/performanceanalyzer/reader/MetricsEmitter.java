/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectField;
import org.jooq.SelectHavingStep;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.DBUtils;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.config.TroubleshootingConfig;
import org.opensearch.performanceanalyzer.metricsdb.Dimensions;
import org.opensearch.performanceanalyzer.metricsdb.Metric;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;

@SuppressWarnings("serial")
public class MetricsEmitter {

    private static final Logger LOG = LogManager.getLogger(MetricsEmitter.class);

    private static final Pattern GC_PATTERN = Pattern.compile(".*(GC|CMS|Parallel).*");
    private static final Pattern REFRESH_PATTERN = Pattern.compile(".*opensearch.*\\[refresh\\].*");
    private static final Pattern MANAGEMENT_PATTERN =
            Pattern.compile(".*opensearch.*\\[management\\].*");
    private static final Pattern MERGE_PATTERN =
            Pattern.compile(".*opensearch\\[.*\\]\\[\\[(.*)\\]\\[(.*)\\].*Lucene Merge.*");
    private static final Pattern SEARCH_PATTERN = Pattern.compile(".*opensearch.*\\[search\\].*");
    private static final Pattern BULK_PATTERN = Pattern.compile(".*opensearch.*\\[bulk\\].*");
    private static final Pattern GENERIC_PATTERN = Pattern.compile(".*opensearch.*\\[generic\\].*");
    private static final Pattern GET_PATTERN = Pattern.compile(".*opensearch.*\\[get\\].*");
    private static final Pattern SNAPSHOT_PATTERN =
            Pattern.compile(".*opensearch.*\\[(snapshot|snapshot_segments)\\].*");
    private static final Pattern FLUSH_PATTERN = Pattern.compile(".*opensearch.*\\[flush\\].*");
    // Version 6.4 onwards uses write threadpool.
    private static final Pattern WRITE_PATTERN = Pattern.compile(".*opensearch.*\\[write\\].*");
    private static final Pattern HTTP_SERVER_PATTERN =
            Pattern.compile(".*opensearch.*\\[http_server_worker\\].*");
    private static final Pattern TRANS_WORKER_PATTERN =
            Pattern.compile(".*opensearch.*\\[transport_worker.*");

    private static final List<String> LATENCY_TABLE_DIMENSIONS =
            new ArrayList<String>() {
                {
                    this.add(ShardRequestMetricsSnapshot.Fields.OPERATION.toString());
                    this.add(HttpRequestMetricsSnapshot.Fields.EXCEPTION.toString());
                    this.add(HttpRequestMetricsSnapshot.Fields.INDICES.toString());
                    this.add(HttpRequestMetricsSnapshot.Fields.HTTP_RESP_CODE.toString());
                    this.add(ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString());
                    this.add(ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString());
                    this.add(ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString());
                }
            };

    private static final List<String> SHARD_STATE_TABLE_DIMENSIONS =
            new ArrayList<String>() {
                {
                    this.add(AllMetrics.ShardStateDimension.INDEX_NAME.toString());
                    this.add(AllMetrics.ShardStateDimension.SHARD_ID.toString());
                    this.add(AllMetrics.ShardStateDimension.SHARD_TYPE.toString());
                    this.add(AllMetrics.ShardStateDimension.NODE_NAME.toString());
                    this.add(AllMetrics.ShardStateDimension.SHARD_STATE.toString());
                }
            };

    private static final List<String> FAULT_DETECTION_TABLE_DIMENSIONS =
            new ArrayList<String>() {
                {
                    this.add(AllMetrics.FaultDetectionDimension.SOURCE_NODE_ID.toString());
                    this.add(AllMetrics.FaultDetectionDimension.TARGET_NODE_ID.toString());
                }
            };

    public static void emitAggregatedOSMetrics(
            final DSLContext create,
            final MetricsDB db,
            final OSMetricsSnapshot osMetricsSnap,
            final ShardRequestMetricsSnapshot rqMetricsSnap)
            throws Exception {

        SelectHavingStep<Record> rqTable = rqMetricsSnap.fetchThreadUtilizationRatioTable();
        SelectHavingStep<Record> osTable = osMetricsSnap.selectAll();

        List<SelectField<?>> fields =
                new ArrayList<SelectField<?>>() {
                    {
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.SHARD_ID
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.INDEX_NAME
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.OPERATION
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.SHARD_ROLE
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(OSMetricsSnapshot.Fields.tName.toString()),
                                        String.class));
                    }
                };

        for (AllMetrics.OSMetrics metric : AllMetrics.OSMetrics.values()) {
            fields.add(
                    DSL.field(ShardRequestMetricsSnapshot.Fields.TUTIL.toString(), Double.class)
                            .mul(DSL.field(DSL.name(metric.toString()), Double.class))
                            .as(metric.toString()));
        }

        ArrayList<Field<?>> groupByFields =
                new ArrayList<Field<?>>() {
                    {
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.SHARD_ID
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.INDEX_NAME
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.OPERATION
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.SHARD_ROLE
                                                        .toString()),
                                        String.class));
                    }
                };

        List<SelectField<?>> aggFields =
                new ArrayList<SelectField<?>>() {
                    {
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.SHARD_ID
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.INDEX_NAME
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.OPERATION
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                ShardRequestMetricsSnapshot.Fields.SHARD_ROLE
                                                        .toString()),
                                        String.class));
                        this.add(
                                DSL.field(
                                        DSL.name(OSMetricsSnapshot.Fields.tName.toString()),
                                        String.class));
                    }
                };

        for (AllMetrics.OSMetrics metric : AllMetrics.OSMetrics.values()) {
            aggFields.add(
                    DSL.sum(DSL.field(DSL.name(metric.toString()), Double.class))
                            .as(MetricsDB.SUM + "_" + metric.toString()));
            aggFields.add(
                    DSL.avg(DSL.field(DSL.name(metric.toString()), Double.class))
                            .as(MetricsDB.AVG + "_" + metric.toString()));
            aggFields.add(
                    DSL.min(DSL.field(DSL.name(metric.toString()), Double.class))
                            .as(MetricsDB.MIN + "_" + metric.toString()));
            aggFields.add(
                    DSL.max(DSL.field(DSL.name(metric.toString()), Double.class))
                            .as(MetricsDB.MAX + "_" + metric.toString()));
        }

        long mCurrT = System.currentTimeMillis();
        Result<Record> res =
                create.select(aggFields)
                        .from(
                                create.select(fields)
                                        .from(rqTable)
                                        .join(osTable)
                                        .on(
                                                osTable.field(
                                                                OSMetricsSnapshot.Fields.tid
                                                                        .toString(),
                                                                String.class)
                                                        .eq(
                                                                rqTable.field(
                                                                        OSMetricsSnapshot.Fields.tid
                                                                                .toString(),
                                                                        String.class))))
                        .groupBy(groupByFields)
                        .fetch();
        long mFinalT = System.currentTimeMillis();
        LOG.debug("Total time taken for tid corelation: {}", mFinalT - mCurrT);
        checkInvalidData(rqTable, osTable, create);

        Set<String> metricColumns = osMetricsSnap.getMetricColumns();

        mCurrT = System.currentTimeMillis();
        for (String metricColumn : metricColumns) {
            List<String> dims =
                    new ArrayList<String>() {
                        {
                            this.add(AllMetrics.CommonDimension.SHARD_ID.toString());
                            this.add(AllMetrics.CommonDimension.INDEX_NAME.toString());
                            this.add(AllMetrics.CommonDimension.OPERATION.toString());
                            this.add(AllMetrics.CommonDimension.SHARD_ROLE.toString());
                            this.add(AllMetrics.CommonDimension.THREAD_NAME.toString());
                        }
                    };
            db.createMetric(new Metric<Double>(metricColumn, 0d), dims);
            BatchBindStep handle = db.startBatchPut(new Metric<Double>(metricColumn, 0d), dims);
            for (Record r : res) {
                if (r.get(MetricsDB.SUM + "_" + metricColumn) == null
                        || r.get(OSMetricsSnapshot.Fields.tName.toString()) == null) {
                    continue;
                }

                Double sumMetric =
                        Double.parseDouble(r.get(MetricsDB.SUM + "_" + metricColumn).toString());
                Double avgMetric =
                        Double.parseDouble(r.get(MetricsDB.AVG + "_" + metricColumn).toString());
                Double minMetric =
                        Double.parseDouble(r.get(MetricsDB.MIN + "_" + metricColumn).toString());
                Double maxMetric =
                        Double.parseDouble(r.get(MetricsDB.MAX + "_" + metricColumn).toString());
                handle.bind(
                        r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString()).toString(),
                        r.get(ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString()).toString(),
                        r.get(ShardRequestMetricsSnapshot.Fields.OPERATION.toString()).toString(),
                        r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString()).toString(),
                        r.get(OSMetricsSnapshot.Fields.tName.toString()).toString(),
                        sumMetric,
                        avgMetric,
                        minMetric,
                        maxMetric);
            }

            if (handle.size() > 0) {
                handle.execute();
            }
        }
        mFinalT = System.currentTimeMillis();
        LOG.debug("Total time taken for writing resource metrics metricsdb: {}", mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.AGGREGATED_OS_METRICS_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    /**
     * Check if there is any invalid data. Invalid data is if we have tid in request table but not
     * in OS tables.
     *
     * @param rqTable request table select
     * @param osTable OS table select
     * @param create db connection
     */
    @SuppressWarnings("unchecked")
    private static void checkInvalidData(
            SelectHavingStep<Record> rqTable,
            SelectHavingStep<Record> osTable,
            final DSLContext create) {
        if (!TroubleshootingConfig.getEnableDevAssert()) {
            return;
        }

        Field tidField = DSL.field(DSL.name(OSMetricsSnapshot.Fields.tid.toString()), String.class);

        Set<String> rqSet =
                DBUtils.getRecordSetByField(rqTable, tidField, DSL.trueCondition(), create);
        Set<String> osSet =
                DBUtils.getRecordSetByField(osTable, tidField, DSL.trueCondition(), create);

        if (!osSet.containsAll(rqSet)) {
            String msg =
                    String.format(
                            "[Invalid Data] Unmatched tid between %s and %s",
                            rqSet.toString(), osSet.toString());
            LOG.error(msg);
            LOG.error(create.select().from(rqTable).fetch().toString());
            LOG.error(create.select().from(osTable).where(DSL.trueCondition()).fetch().toString());
            throw new RuntimeException(msg);
        }
    }

    public static void emitWorkloadMetrics(
            final DSLContext create,
            final MetricsDB db,
            final ShardRequestMetricsSnapshot rqMetricsSnap)
            throws Exception {
        long mCurrT = System.currentTimeMillis();
        Result<Record> res = rqMetricsSnap.fetchLatencyByOp();

        db.createMetric(
                new Metric<Double>(AllMetrics.CommonMetric.LATENCY.toString(), 0d),
                LATENCY_TABLE_DIMENSIONS);
        BatchBindStep handle =
                db.startBatchPut(
                        new Metric<Double>(AllMetrics.CommonMetric.LATENCY.toString(), 0d),
                        LATENCY_TABLE_DIMENSIONS);

        // Dims need to be changed.
        List<String> shardDims =
                new ArrayList<String>() {
                    {
                        this.add(ShardRequestMetricsSnapshot.Fields.OPERATION.toString());
                        this.add(ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString());
                        this.add(ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString());
                        this.add(ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString());
                    }
                };

        db.createMetric(
                new Metric<Double>(AllMetrics.ShardOperationMetric.SHARD_OP_COUNT.toString(), 0d),
                shardDims);
        BatchBindStep countHandle =
                db.startBatchPut(
                        new Metric<Double>(
                                AllMetrics.ShardOperationMetric.SHARD_OP_COUNT.toString(), 0d),
                        shardDims);

        db.createMetric(
                new Metric<Double>(AllMetrics.ShardBulkMetric.DOC_COUNT.toString(), 0d), shardDims);
        BatchBindStep bulkDocHandle =
                db.startBatchPut(
                        new Metric<Double>(AllMetrics.ShardBulkMetric.DOC_COUNT.toString(), 0d),
                        shardDims);

        for (Record r : res) {
            Double sumLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    ShardRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());
            Double avgLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    ShardRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());
            Double minLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    ShardRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());
            Double maxLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    ShardRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            handle.bind(
                    r.get(ShardRequestMetricsSnapshot.Fields.OPERATION.toString()).toString(),
                    null,
                    null,
                    null,
                    r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString()).toString(),
                    r.get(ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString()).toString(),
                    r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString()).toString(),
                    sumLatency,
                    avgLatency,
                    minLatency,
                    maxLatency);

            Double count =
                    Double.parseDouble(
                            r.get(AllMetrics.ShardOperationMetric.SHARD_OP_COUNT.toString())
                                    .toString());
            countHandle.bind(
                    r.get(ShardRequestMetricsSnapshot.Fields.OPERATION.toString()).toString(),
                    r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString()).toString(),
                    r.get(ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString()).toString(),
                    r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString()).toString(),
                    count,
                    count,
                    count,
                    count);

            Object bulkDocCountObj = r.get(AllMetrics.ShardBulkMetric.DOC_COUNT.toString());
            if (bulkDocCountObj != null) {
                Double bulkDocCount = Double.parseDouble(bulkDocCountObj.toString());
                bulkDocHandle.bind(
                        r.get(ShardRequestMetricsSnapshot.Fields.OPERATION.toString()).toString(),
                        r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString()).toString(),
                        r.get(ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString()).toString(),
                        r.get(ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString()).toString(),
                        bulkDocCount,
                        bulkDocCount,
                        bulkDocCount,
                        bulkDocCount);
            }
        }
        if (handle.size() > 0) {
            handle.execute();
        }
        if (countHandle.size() > 0) {
            countHandle.execute();
        }
        if (bulkDocHandle.size() > 0) {
            bulkDocHandle.execute();
        }
        long mFinalT = System.currentTimeMillis();
        LOG.debug("Total time taken for writing workload metrics metricsdb: {}", mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.WORKLOAD_METRICS_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    public static void emitThreadNameMetrics(
            final DSLContext create, final MetricsDB db, final OSMetricsSnapshot osMetricsSnap)
            throws Exception {
        long mCurrT = System.currentTimeMillis();
        Result<Record> res = osMetricsSnap.getOSMetrics();

        Set<String> metricColumns = osMetricsSnap.getMetricColumns();
        for (Record r : res) {
            Dimensions dimensions = new Dimensions();
            Object threadName = r.get(OSMetricsSnapshot.Fields.tName.toString());

            if (threadName == null) {
                LOG.debug("Could not find tName: {}", r);
                continue;
            }
            String operation = categorizeThreadName(threadName.toString(), dimensions);
            if (operation == null) {
                continue;
            }
            dimensions.put(
                    AllMetrics.CommonDimension.THREAD_NAME.toString(), threadName.toString());
            dimensions.put(ShardRequestMetricsSnapshot.Fields.OPERATION.toString(), operation);
            for (String metricColumn : metricColumns) {
                if (r.get(metricColumn) == null) {
                    continue;
                }
                Double metric = Double.parseDouble(r.get(metricColumn).toString());
                if (operation.equals("merge") && metricColumn.equals("cpu")) {
                    LOG.debug("Putting merge metric {}", metric);
                }
                db.putMetric(new Metric<Double>(metricColumn, metric), dimensions, 0);
            }
        }
        long mFinalT = System.currentTimeMillis();
        LOG.debug(
                "Total time taken for writing threadName metrics metricsdb: {}", mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.THREAD_NAME_METRICS_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    public static String categorizeThreadName(String threadName, Dimensions dimensions) {
        // shardSearch and shardBulk os metrics are emitted by emitAggregatedOSMetrics and
        // emitWorkloadMetrics functions.
        // Hence these are ignored in this emitter.
        if (SEARCH_PATTERN.matcher(threadName).matches()) {
            return "search";
        }
        if (BULK_PATTERN.matcher(threadName).matches()
                || WRITE_PATTERN.matcher(threadName).matches()) {
            return "write";
        }

        if (GC_PATTERN.matcher(threadName).matches()) {
            return "GC";
        }
        if (REFRESH_PATTERN.matcher(threadName).matches()) {
            return "refresh";
        }
        if (MANAGEMENT_PATTERN.matcher(threadName).matches()) {
            return "management";
        }
        if (HTTP_SERVER_PATTERN.matcher(threadName).matches()) {
            return "httpServer";
        }
        if (TRANS_WORKER_PATTERN.matcher(threadName).matches()) {
            return "transportWorker";
        }
        if (GENERIC_PATTERN.matcher(threadName).matches()) {
            return "generic";
        }
        if (FLUSH_PATTERN.matcher(threadName).matches()) {
            return "flush";
        }
        if (SNAPSHOT_PATTERN.matcher(threadName).matches()) {
            return "snapshot";
        }
        if (GET_PATTERN.matcher(threadName).matches()) {
            return "get";
        }

        Matcher mergeMatcher = MERGE_PATTERN.matcher(threadName);
        if (mergeMatcher.matches()) {
            dimensions.put(
                    ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString(),
                    mergeMatcher.group(1));
            dimensions.put(
                    ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString(), mergeMatcher.group(2));
            return "merge";
        }
        return "other";
    }

    public static void emitHttpMetrics(
            final DSLContext create,
            final MetricsDB db,
            final HttpRequestMetricsSnapshot rqMetricsSnap)
            throws Exception {
        long mCurrT = System.currentTimeMillis();
        Dimensions dimensions = new Dimensions();
        Result<Record> res = rqMetricsSnap.fetchLatencyByOp();
        List<String> dims =
                new ArrayList<String>() {
                    {
                        this.add(HttpRequestMetricsSnapshot.Fields.OPERATION.toString());
                        this.add(HttpRequestMetricsSnapshot.Fields.EXCEPTION.toString());
                        this.add(HttpRequestMetricsSnapshot.Fields.INDICES.toString());
                        this.add(HttpRequestMetricsSnapshot.Fields.HTTP_RESP_CODE.toString());
                    }
                };

        db.createMetric(
                new Metric<Double>(AllMetrics.CommonMetric.LATENCY.toString(), 0d),
                LATENCY_TABLE_DIMENSIONS);

        db.createMetric(
                new Metric<Double>(AllMetrics.HttpMetric.HTTP_TOTAL_REQUESTS.toString(), 0d), dims);
        db.createMetric(
                new Metric<Double>(AllMetrics.HttpMetric.HTTP_REQUEST_DOCS.toString(), 0d), dims);

        for (Record r : res) {
            dimensions.put(
                    HttpRequestMetricsSnapshot.Fields.OPERATION.toString(),
                    r.get(HttpRequestMetricsSnapshot.Fields.OPERATION.toString()).toString());
            dimensions.put(
                    HttpRequestMetricsSnapshot.Fields.HTTP_RESP_CODE.toString(),
                    r.get(HttpRequestMetricsSnapshot.Fields.HTTP_RESP_CODE.toString()).toString());
            dimensions.put(
                    HttpRequestMetricsSnapshot.Fields.INDICES.toString(),
                    r.get(HttpRequestMetricsSnapshot.Fields.INDICES.toString()).toString());
            dimensions.put(
                    HttpRequestMetricsSnapshot.Fields.EXCEPTION.toString(),
                    r.get(HttpRequestMetricsSnapshot.Fields.EXCEPTION.toString()).toString());

            Double sumLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());
            Double avgLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());
            Double minLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());
            Double maxLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            Double count =
                    Double.parseDouble(
                            r.get(HttpRequestMetricsSnapshot.Fields.HTTP_TOTAL_REQUESTS.toString())
                                    .toString());

            Double sumItemCount =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields
                                                            .HTTP_REQUEST_DOCS
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());
            Double avgItemCount =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields
                                                            .HTTP_REQUEST_DOCS
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());
            Double minItemCount =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields
                                                            .HTTP_REQUEST_DOCS
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());
            Double maxItemCount =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    HttpRequestMetricsSnapshot.Fields
                                                            .HTTP_REQUEST_DOCS
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            db.putMetric(
                    new Metric<Double>(
                            AllMetrics.CommonMetric.LATENCY.toString(),
                            sumLatency,
                            avgLatency,
                            minLatency,
                            maxLatency),
                    dimensions,
                    0);
            db.putMetric(
                    new Metric<Double>(AllMetrics.HttpMetric.HTTP_TOTAL_REQUESTS.toString(), count),
                    dimensions,
                    0);
            db.putMetric(
                    new Metric<Double>(
                            AllMetrics.HttpMetric.HTTP_REQUEST_DOCS.toString(),
                            sumItemCount,
                            avgItemCount,
                            minItemCount,
                            maxItemCount),
                    dimensions,
                    0);
        }

        long mFinalT = System.currentTimeMillis();
        LOG.debug("Total time taken for writing http metrics metricsdb: {}", mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.HTTP_METRICS_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    public static void emitGarbageCollectionInfo(
            MetricsDB metricsDB, GarbageCollectorInfoSnapshot garbageCollectorInfoSnapshot) {
        long mCurrT = System.currentTimeMillis();
        Result<Record> gcTypeRecords = garbageCollectorInfoSnapshot.fetchAll();

        List<String> dims =
                new ArrayList<String>() {
                    {
                        this.add(AllMetrics.GCInfoDimension.MEMORY_POOL.toString());
                        this.add(AllMetrics.GCInfoDimension.COLLECTOR_NAME.toString());
                    }
                };

        metricsDB.createMetric(
                new Metric<>(AllMetrics.GCInfoValue.GARBAGE_COLLECTOR_TYPE.toString(), 0d), dims);
        BatchBindStep handle =
                metricsDB.startBatchPut(
                        new Metric<>(AllMetrics.GCInfoValue.GARBAGE_COLLECTOR_TYPE.toString(), 0d),
                        dims);
        for (Record record : gcTypeRecords) {
            Optional<Object> memPoolObj =
                    Optional.ofNullable(
                            record.get(AllMetrics.GCInfoDimension.MEMORY_POOL.toString()));
            Optional<Object> collectorObj =
                    Optional.ofNullable(
                            record.get(AllMetrics.GCInfoDimension.COLLECTOR_NAME.toString()));
            handle.bind(
                    memPoolObj.orElseGet(Object::new).toString(),
                    collectorObj.orElseGet(Object::new).toString(),
                    // the rest are agg fields: sum, avg, min, max which don't make sense for gc
                    // type.
                    null,
                    null,
                    null,
                    null);
        }

        handle.execute();

        long mFinalT = System.currentTimeMillis();
        LOG.debug(
                "Total time taken for writing garbage collection info into metricsDB: {}",
                mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.GC_INFO_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    public static void emitAdmissionControlMetrics(
            MetricsDB metricsDB, AdmissionControlSnapshot snapshot) {
        long mCurrT = System.currentTimeMillis();
        Result<Record> records = snapshot.fetchAll();

        List<String> dims =
                new ArrayList<String>() {
                    {
                        this.add(AllMetrics.AdmissionControlDimension.CONTROLLER_NAME.toString());
                    }
                };

        metricsDB.createMetric(
                new Metric<Double>(AllMetrics.AdmissionControlValue.REJECTION_COUNT.toString(), 0d),
                dims);
        BatchBindStep handle =
                metricsDB.startBatchPut(
                        new Metric<Double>(
                                AllMetrics.AdmissionControlValue.REJECTION_COUNT.toString(), 0d),
                        dims);

        for (Record record : records) {
            Optional<Object> controllerObj =
                    Optional.ofNullable(
                            record.get(
                                    AllMetrics.AdmissionControlDimension.CONTROLLER_NAME
                                            .toString()));
            Optional<Object> rejectionCountObj =
                    Optional.ofNullable(
                            record.get(
                                    AllMetrics.AdmissionControlValue.REJECTION_COUNT.toString()));

            if (controllerObj.isPresent() && rejectionCountObj.isPresent()) {
                handle.bind(
                        controllerObj.orElseGet(Object::new).toString(),
                        // the rest are agg fields: sum, avg, min, max
                        rejectionCountObj.map(o -> Long.parseLong(o.toString())).orElse(0L),
                        rejectionCountObj.map(o -> Long.parseLong(o.toString())).orElse(0L),
                        rejectionCountObj.map(o -> Long.parseLong(o.toString())).orElse(0L),
                        rejectionCountObj.map(o -> Long.parseLong(o.toString())).orElse(0L));
            }
        }

        handle.execute();

        long mFinalT = System.currentTimeMillis();
        LOG.debug(
                "Total time taken for writing AdmissionControl into metricsDB: {}",
                mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.ADMISSION_CONTROL_METRICS_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    public static void emitClusterManagerEventMetrics(
            MetricsDB metricsDB,
            ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshot) {

        long mCurrT = System.currentTimeMillis();
        Result<Record> queueAndRunTimeResult =
                clusterManagerEventMetricsSnapshot.fetchQueueAndRunTime();

        List<String> dims =
                new ArrayList<String>() {
                    {
                        this.add(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_INSERT_ORDER
                                        .toString());
                        this.add(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_PRIORITY
                                        .toString());
                        this.add(
                                AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_TYPE
                                        .toString());
                        this.add(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_METADATA
                                        .toString());
                    }
                };

        emitQueueTimeMetric(metricsDB, queueAndRunTimeResult, dims);
        emitRuntimeMetric(metricsDB, queueAndRunTimeResult, dims);

        long mFinalT = System.currentTimeMillis();
        LOG.debug(
                "Total time taken for writing cluster_manager event queue metrics metricsdb: {}",
                mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.CLUSTER_MANAGER_EVENT_METRICS_EMITTER_EXECUTION_TIME,
                mFinalT - mCurrT);
    }

    // TODO: Refactor and remove this out into metric-specific emitter
    private static void emitRuntimeMetric(
            MetricsDB metricsDB, Result<Record> res, List<String> dims) {

        metricsDB.createMetric(
                new Metric<Double>(
                        AllMetrics.ClusterManagerMetricValues.CLUSTER_MANAGER_TASK_RUN_TIME
                                .toString(),
                        0d),
                dims);

        BatchBindStep handle =
                metricsDB.startBatchPut(
                        new Metric<Double>(
                                AllMetrics.ClusterManagerMetricValues.CLUSTER_MANAGER_TASK_RUN_TIME
                                        .toString(),
                                0d),
                        dims);

        for (Record r : res) {

            Double sumQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_RUN_TIME
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());

            Double avgQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_RUN_TIME
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());

            Double minQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_RUN_TIME
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());

            Double maxQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_RUN_TIME
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            handle.bind(
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_INSERT_ORDER
                                            .toString())
                            .toString(),
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_PRIORITY
                                            .toString())
                            .toString(),
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_TYPE
                                            .toString())
                            .toString(),
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_METADATA
                                            .toString())
                            .toString(),
                    sumQueueTime,
                    avgQueueTime,
                    minQueueTime,
                    maxQueueTime);
        }

        handle.execute();
    }

    // TODO: Refactor and remove this out into metric-specific emitter
    private static void emitQueueTimeMetric(
            MetricsDB metricsDB, Result<Record> res, List<String> dims) {

        metricsDB.createMetric(
                new Metric<Double>(
                        AllMetrics.ClusterManagerMetricValues.CLUSTER_MANAGER_TASK_QUEUE_TIME
                                .toString(),
                        0d),
                dims);

        BatchBindStep handle =
                metricsDB.startBatchPut(
                        new Metric<Double>(
                                AllMetrics.ClusterManagerMetricValues
                                        .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                        .toString(),
                                0d),
                        dims);

        for (Record r : res) {

            Double sumQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());

            Double avgQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());

            Double minQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());

            Double maxQueueTime =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerMetricDimensions
                                                            .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            handle.bind(
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_INSERT_ORDER
                                            .toString())
                            .toString(),
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_PRIORITY
                                            .toString())
                            .toString(),
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_TYPE
                                            .toString())
                            .toString(),
                    r.get(
                                    AllMetrics.ClusterManagerMetricDimensions
                                            .CLUSTER_MANAGER_TASK_METADATA
                                            .toString())
                            .toString(),
                    sumQueueTime,
                    avgQueueTime,
                    minQueueTime,
                    maxQueueTime);
        }

        handle.execute();
    }

    /**
     * TODO: Some of these metrics have default value like tcp.SSThresh:-1. Should we count them in
     * aggregation?
     *
     * @param create A contextual DSL providing "attached" implementations to the org.jooq
     *     interfaces.
     * @param db On-disk database that holds a snapshot of all metrics, which includes the metrics
     *     that customers can query.
     * @param snap In memory database that holds a snapshot of all metrics. This is the intermediate
     *     representation of metrics.
     * @throws Exception thrown when we cannot emit metrics from the in-memory database to the
     *     on-disk database.
     */
    public static void emitNodeMetrics(
            final DSLContext create, final MetricsDB db, final MemoryDBSnapshot snap)
            throws Exception {

        Map<String, SelectHavingStep<Record>> metadataTable = snap.selectMetadataSource();

        Map<String, List<Field<?>>> selectField = snap.getTableSelectFieldsMap();

        List<String> dimensionNames = snap.getDimensionNames();

        for (Map.Entry<String, SelectHavingStep<Record>> entry : metadataTable.entrySet()) {
            long mCurrT = System.currentTimeMillis();

            String tableName = entry.getKey();
            Result<Record> fetchedData = entry.getValue().fetch();

            long mFinalT = System.currentTimeMillis();
            LOG.debug("Total time taken for aggregating {} : {}", tableName, mFinalT - mCurrT);

            if (fetchedData == null || fetchedData.size() == 0) {
                LOG.debug("No data to emit: {}", tableName);
                continue;
            }

            mCurrT = System.currentTimeMillis();

            List<Field<?>> selectFields = selectField.get(tableName);

            db.createMetric(new Metric<Double>(tableName, 0d), dimensionNames);

            BatchBindStep handle = db.startBatchPut(tableName, selectFields.size());
            for (Record r : fetchedData) {
                int columnNum = selectFields.size();
                Object[] bindValues = new Object[columnNum];
                for (int i = 0; i < columnNum; i++) {
                    bindValues[i] = r.get(selectFields.get(i).getName());
                }
                handle.bind(bindValues);
            }
            handle.execute();

            mFinalT = System.currentTimeMillis();
            LOG.debug(
                    "Total time taken for writing {} metrics metricsdb: {}",
                    tableName,
                    mFinalT - mCurrT);
            ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                    ReaderMetrics.NODE_METRICS_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
        }
    }

    public static void emitFaultDetectionMetrics(
            MetricsDB db, FaultDetectionMetricsSnapshot faultDetectionSnapshot) {

        long mCurrT = System.currentTimeMillis();
        Dimensions dimensions = new Dimensions();
        Result<Record> res = faultDetectionSnapshot.fetchAggregatedTable();

        db.createMetric(
                new Metric<Double>(
                        AllMetrics.FaultDetectionMetric.FOLLOWER_CHECK_LATENCY.toString(), 0d),
                FAULT_DETECTION_TABLE_DIMENSIONS);

        db.createMetric(
                new Metric<Double>(
                        AllMetrics.FaultDetectionMetric.LEADER_CHECK_LATENCY.toString(), 0d),
                FAULT_DETECTION_TABLE_DIMENSIONS);

        db.createMetric(
                new Metric<Double>(
                        AllMetrics.FaultDetectionMetric.FOLLOWER_CHECK_FAILURE.toString(), 0d),
                FAULT_DETECTION_TABLE_DIMENSIONS);

        db.createMetric(
                new Metric<Double>(
                        AllMetrics.FaultDetectionMetric.LEADER_CHECK_FAILURE.toString(), 0d),
                FAULT_DETECTION_TABLE_DIMENSIONS);
        for (Record r : res) {
            dimensions.put(
                    AllMetrics.FaultDetectionDimension.SOURCE_NODE_ID.toString(),
                    r.get(AllMetrics.FaultDetectionDimension.SOURCE_NODE_ID.toString()).toString());
            dimensions.put(
                    AllMetrics.FaultDetectionDimension.TARGET_NODE_ID.toString(),
                    r.get(AllMetrics.FaultDetectionDimension.TARGET_NODE_ID.toString()).toString());

            Double sumLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());
            Double avgLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());
            Double minLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());
            Double maxLatency =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.LAT
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            Double sumFault =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.FAULT
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());
            Double avgFault =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.FAULT
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());
            Double minFault =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.FAULT
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());
            Double maxFault =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    FaultDetectionMetricsSnapshot.Fields.FAULT
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());
            if (r.get(FaultDetectionMetricsSnapshot.Fields.FAULT_DETECTION_TYPE.toString())
                    .toString()
                    .equals(PerformanceAnalyzerMetrics.FAULT_DETECTION_FOLLOWER_CHECK)) {
                db.putMetric(
                        new Metric<Double>(
                                AllMetrics.FaultDetectionMetric.FOLLOWER_CHECK_LATENCY.toString(),
                                sumLatency,
                                avgLatency,
                                minLatency,
                                maxLatency),
                        dimensions,
                        0);
                db.putMetric(
                        new Metric<Double>(
                                AllMetrics.FaultDetectionMetric.FOLLOWER_CHECK_FAILURE.toString(),
                                sumFault,
                                avgFault,
                                minFault,
                                maxFault),
                        dimensions,
                        0);
            } else if (r.get(FaultDetectionMetricsSnapshot.Fields.FAULT_DETECTION_TYPE.toString())
                    .toString()
                    .equals(PerformanceAnalyzerMetrics.FAULT_DETECTION_LEADER_CHECK)) {
                db.putMetric(
                        new Metric<Double>(
                                AllMetrics.FaultDetectionMetric.LEADER_CHECK_LATENCY.toString(),
                                sumLatency,
                                avgLatency,
                                minLatency,
                                maxLatency),
                        dimensions,
                        0);
                db.putMetric(
                        new Metric<Double>(
                                AllMetrics.FaultDetectionMetric.LEADER_CHECK_FAILURE.toString(),
                                sumFault,
                                avgFault,
                                minFault,
                                maxFault),
                        dimensions,
                        0);
            }
        }
        long mFinalT = System.currentTimeMillis();
        LOG.debug(
                "Total time taken for writing fault detection metrics to metricsdb: {}",
                mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.FAULT_DETECTION_METRICS_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    public static void emitClusterManagerThrottledTaskMetric(
            MetricsDB metricsDB,
            ClusterManagerThrottlingMetricsSnapshot clusterManagerThrottlingMetricsSnapshot) {
        long mCurrT = System.currentTimeMillis();
        Result<Record> clusterManagerThrottlingMetrics =
                clusterManagerThrottlingMetricsSnapshot.fetchAggregatedMetrics();

        List<String> dims = new ArrayList<String>();
        emitClusterManagerThrottlingCount(metricsDB, clusterManagerThrottlingMetrics, dims);
        emitDataThrottlingRetryingCount(metricsDB, clusterManagerThrottlingMetrics, dims);

        long mFinalT = System.currentTimeMillis();
        LOG.debug(
                "Total time taken for writing cluster_manager throttling metrics metricsdb: {}",
                mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.CLUSTER_MANAGER_THROTTLING_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }

    // TODO: Refactor and remove this out into metric-specific emitter
    public static void emitClusterManagerThrottlingCount(
            MetricsDB metricsDB, Result<Record> res, List<String> dims) {
        metricsDB.createMetric(
                new Metric<Double>(
                        AllMetrics.ClusterManagerThrottlingValue
                                .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                .toString(),
                        0d),
                dims);

        BatchBindStep handle =
                metricsDB.startBatchPut(
                        new Metric<Double>(
                                AllMetrics.ClusterManagerThrottlingValue
                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                        .toString(),
                                0d),
                        dims);

        for (Record r : res) {

            Double sumClusterManagerThrottledTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());

            Double avgClusterManagerThrottledTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());

            Double minClusterManagerThrottledTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());

            Double maxClusterManagerThrottledTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            handle.bind(
                    sumClusterManagerThrottledTask,
                    avgClusterManagerThrottledTask,
                    minClusterManagerThrottledTask,
                    maxClusterManagerThrottledTask);
        }

        handle.execute();
    }

    // TODO: Refactor and remove this out into metric-specific emitter
    public static void emitDataThrottlingRetryingCount(
            MetricsDB metricsDB, Result<Record> res, List<String> dims) {
        metricsDB.createMetric(
                new Metric<Double>(
                        AllMetrics.ClusterManagerThrottlingValue.DATA_RETRYING_TASK_COUNT
                                .toString(),
                        0d),
                dims);

        BatchBindStep handle =
                metricsDB.startBatchPut(
                        new Metric<Double>(
                                AllMetrics.ClusterManagerThrottlingValue.DATA_RETRYING_TASK_COUNT
                                        .toString(),
                                0d),
                        dims);

        for (Record r : res) {

            Double sumDataRetryingTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .DATA_RETRYING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.SUM))
                                    .toString());

            Double avgDataRetryingTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .DATA_RETRYING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.AVG))
                                    .toString());

            Double minDataRetryingTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .DATA_RETRYING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.MIN))
                                    .toString());

            Double maxDataRetryingTask =
                    Double.parseDouble(
                            r.get(
                                            DBUtils.getAggFieldName(
                                                    AllMetrics.ClusterManagerThrottlingValue
                                                            .DATA_RETRYING_TASK_COUNT
                                                            .toString(),
                                                    MetricsDB.MAX))
                                    .toString());

            handle.bind(
                    sumDataRetryingTask,
                    avgDataRetryingTask,
                    minDataRetryingTask,
                    maxDataRetryingTask);
        }
        handle.execute();
    }

    public static void emitShardStateMetric(
            MetricsDB metricsDB, ShardStateMetricsSnapshot shardStateMetricsSnapshot) {
        long mCurrT = System.currentTimeMillis();
        Result<Record> shardStateMetrics = shardStateMetricsSnapshot.fetchAll();
        metricsDB.createMetric(
                new Metric<Double>(AllMetrics.ShardStateValue.SHARD_STATE.toString(), 0d),
                SHARD_STATE_TABLE_DIMENSIONS);

        BatchBindStep handle =
                metricsDB.startBatchPut(
                        new Metric<Double>(AllMetrics.ShardStateValue.SHARD_STATE.toString(), 0d),
                        SHARD_STATE_TABLE_DIMENSIONS);

        for (Record r : shardStateMetrics) {
            handle.bind(
                    r.get(AllMetrics.ShardStateDimension.INDEX_NAME.toString()).toString(),
                    r.get(AllMetrics.ShardStateDimension.SHARD_ID.toString()).toString(),
                    r.get(AllMetrics.ShardStateDimension.SHARD_TYPE.toString()).toString(),
                    r.get(AllMetrics.ShardStateDimension.NODE_NAME.toString()).toString(),
                    r.get(AllMetrics.ShardStateDimension.SHARD_STATE.toString()).toString(),
                    1.0,
                    1.0,
                    1.0,
                    1.0);
        }
        handle.execute();
        long mFinalT = System.currentTimeMillis();
        LOG.debug(
                "Total time taken for writing shard state event queue metrics metricsdb: {}",
                mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.SHARD_STATE_EMITTER_EXECUTION_TIME, mFinalT - mCurrT);
    }
}
