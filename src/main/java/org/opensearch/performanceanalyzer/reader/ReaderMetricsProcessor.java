/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.config.PluginSettings;
import org.opensearch.performanceanalyzer.commons.event_process.EventDispatcher;
import org.opensearch.performanceanalyzer.commons.event_process.EventLog;
import org.opensearch.performanceanalyzer.commons.event_process.EventLogFileHandler;
import org.opensearch.performanceanalyzer.commons.event_process.EventProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.config.overrides.ConfigOverridesApplier;
import org.opensearch.performanceanalyzer.core.Util;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;

public class ReaderMetricsProcessor implements Runnable {
    private static final Logger LOG = LogManager.getLogger(ReaderMetricsProcessor.class);

    private static final String DB_URL = "jdbc:sqlite:";
    private final Connection conn;
    private final DSLContext create;

    // This semaphore is used to control access to metricsDBMap from threads outside of
    // ReaderMetricsProcessor.
    private NavigableMap<Long, MetricsDB> metricsDBMap;
    private NavigableMap<Long, OSMetricsSnapshot> osMetricsMap;
    private NavigableMap<Long, ShardRequestMetricsSnapshot> shardRqMetricsMap;
    private NavigableMap<Long, HttpRequestMetricsSnapshot> httpRqMetricsMap;
    private NavigableMap<Long, ClusterManagerEventMetricsSnapshot> clusterManagerEventMetricsMap;
    private NavigableMap<Long, GarbageCollectorInfoSnapshot> gcInfoMap;
    private Map<AllMetrics.MetricName, NavigableMap<Long, MemoryDBSnapshot>> nodeMetricsMap;
    private NavigableMap<Long, FaultDetectionMetricsSnapshot> faultDetectionMetricsMap;
    private NavigableMap<Long, ClusterManagerThrottlingMetricsSnapshot>
            clusterManagerThrottlingMetricsMap;
    private NavigableMap<Long, ShardStateMetricsSnapshot> shardStateMetricsMap;
    private NavigableMap<Long, AdmissionControlSnapshot> admissionControlMetricsMap;

    private static final int MAX_DATABASES = 2;
    private static final int OS_SNAPSHOTS = 4;
    private static final int SHARD_STATE_SNAPSHOTS = 2;
    private static final int RQ_SNAPSHOTS = 4;
    private static final int HTTP_RQ_SNAPSHOTS = 4;
    private static final int CLUSTER_MANAGER_EVENT_SNAPSHOTS = 4;
    private static final int FAULT_DETECTION_SNAPSHOTS = 2;
    private static final int GC_INFO_SNAPSHOTS = 4;
    private static final int CLUSTER_MANAGER_THROTTLING_SNAPSHOTS = 2;
    private static final int AC_SNAPSHOTS = 2;
    private final String rootLocation;

    private final AppContext appContext;
    private final ConfigOverridesApplier configOverridesApplier;

    public static final String BATCH_METRICS_ENABLED_CONF_FILE = "batch_metrics_enabled.conf";
    private boolean batchMetricsEnabled;
    public static final boolean defaultBatchMetricsEnabled = false;
    // This needs to be concurrent since it may be concurrently accessed by the metrics processor
    // thread and the query handler thread.
    private ConcurrentSkipListSet<Long> batchMetricsDBSet;

    private final boolean processNewFormat;
    private final EventLogFileHandler eventLogFileHandler;
    // This needs to be volatile to avoid failure caused by thread local cached values.
    private static volatile ReaderMetricsProcessor current = null;

    public static void setCurrentInstance(ReaderMetricsProcessor currentInstance) {
        current = currentInstance;
    }

    public static ReaderMetricsProcessor getInstance() {
        return current;
    }

    public ReaderMetricsProcessor(String rootLocation) throws Exception {
        this(rootLocation, false, null);
    }

    public ReaderMetricsProcessor(
            String rootLocation, boolean processNewFormat, final AppContext appContext)
            throws Exception {
        conn = DriverManager.getConnection(DB_URL);
        create = DSL.using(conn, SQLDialect.SQLITE);
        metricsDBMap = new ConcurrentSkipListMap<>();
        osMetricsMap = new TreeMap<>();
        shardRqMetricsMap = new TreeMap<>();
        httpRqMetricsMap = new TreeMap<>();
        clusterManagerEventMetricsMap = new TreeMap<>();
        faultDetectionMetricsMap = new TreeMap<>();
        shardStateMetricsMap = new TreeMap<>();
        gcInfoMap = new TreeMap<>();
        clusterManagerThrottlingMetricsMap = new TreeMap<>();
        admissionControlMetricsMap = new TreeMap<>();
        this.rootLocation = rootLocation;
        this.configOverridesApplier = new ConfigOverridesApplier();

        AllMetrics.MetricName[] names = AllMetrics.MetricName.values();
        nodeMetricsMap = new HashMap<>(names.length);
        for (int i = 0; i < names.length; i++) {
            nodeMetricsMap.put(names[i], new TreeMap<>());
        }
        eventLogFileHandler = new EventLogFileHandler(new EventLog(), rootLocation);
        this.processNewFormat = processNewFormat;
        this.appContext = appContext;
        batchMetricsEnabled = defaultBatchMetricsEnabled;
        batchMetricsDBSet = new ConcurrentSkipListSet<>();
        readBatchMetricsEnabledFromConf();
        restoreBatchMetricsState();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            Statement stmt = conn.createStatement();
            try {
                stmt.executeUpdate("PRAGMA journal_mode = OFF");
                stmt.executeUpdate("PRAGMA soft_heap_limit = 10000000");
            } catch (Exception e) {
                LOG.error("Unable to run PRAGMA");
            } finally {
                stmt.close();
            }

            long runInterval = MetricsConfiguration.SAMPLING_INTERVAL / 2;

            while (true) {
                // Create snapshots.
                Statement vacuumStmt = conn.createStatement();
                try {
                    vacuumStmt.executeUpdate("VACUUM");
                } catch (Exception e) {
                    LOG.error("Unable to run Vacuum.");
                } finally {
                    vacuumStmt.close();
                }
                conn.setAutoCommit(false);
                startTime = System.currentTimeMillis();
                processMetrics(rootLocation, startTime);
                trimOldSnapshots();
                conn.commit();
                conn.setAutoCommit(true);
                trimOldMetricsDBFiles();
                long duration = System.currentTimeMillis() - startTime;
                LOG.debug("Total time taken: {}", duration);
                if (duration < runInterval) {
                    Thread.sleep(runInterval - duration);
                }
            }
        } catch (Throwable e) {
            StatsCollector.instance()
                    .logException(StatExceptionCode.READER_METRICS_PROCESSOR_ERROR);
            LOG.error(
                    (Supplier<?>)
                            () ->
                                    new ParameterizedMessage(
                                            "READER PROCESSOR ERROR. NEEDS DEBUGGING: {}",
                                            e.toString()),
                    e);
            try {
                long duration = System.currentTimeMillis() - startTime;
                if (duration < MetricsConfiguration.SAMPLING_INTERVAL) {
                    Thread.sleep(MetricsConfiguration.SAMPLING_INTERVAL - duration);
                }
            } catch (Exception ex) {
                LOG.error("Exception in sleep: {}", () -> ex);
            }
            throw new RuntimeException("READER ERROR");
        } finally {
            try {
                shutdown();
                LOG.error("Connection to the database was closed.");
            } catch (Exception e) {
                LOG.error("Unable to close all database connections and shutdown cleanly.");
            }
        }
    }

    public void shutdown() {
        try {
            if (!conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            StatsCollector.instance()
                    .logException(StatExceptionCode.IN_MEMORY_DATABASE_CONN_CLOSURE_ERROR);
            LOG.error("Unable to close inmemory database connection.", e);
        }

        for (MetricsDB db : metricsDBMap.values()) {
            try {
                db.close();
            } catch (Exception e) {
                StatsCollector.instance().logException(StatExceptionCode.METRICS_DB_CLOSURE_ERROR);
                LOG.error("Unable to close database - " + db.getDBFilePath(), e);
            }
        }
    }

    /** Restore batch metrics state based on files from disk. */
    private void restoreBatchMetricsState() {
        Set<Long> recoveredMetricsdbFiles = MetricsDB.listOnDiskFiles();
        boolean shouldCleanup = PluginSettings.instance().shouldCleanupMetricsDBFiles();
        if (batchMetricsEnabled) {
            long minTime =
                    System.currentTimeMillis()
                            - PluginSettings.instance().getBatchMetricsRetentionPeriodMinutes()
                                    * 60
                                    * 1000;
            for (Long ts : recoveredMetricsdbFiles) {
                if (ts >= minTime) {
                    batchMetricsDBSet.add(ts);
                } else if (shouldCleanup) {
                    MetricsDB.deleteOnDiskFile(ts);
                }
            }
        } else if (shouldCleanup) {
            recoveredMetricsdbFiles.forEach(ts -> MetricsDB.deleteOnDiskFile(ts));
        }
    }

    /**
     * Cleans up stale in-memory snapshots.
     *
     * @throws Exception if there is some problem removing a snapshot
     */
    public void trimOldSnapshots() throws Exception {
        trimMap(osMetricsMap, OS_SNAPSHOTS);
        trimMap(shardRqMetricsMap, RQ_SNAPSHOTS);
        trimMap(httpRqMetricsMap, HTTP_RQ_SNAPSHOTS);
        trimMap(clusterManagerEventMetricsMap, CLUSTER_MANAGER_EVENT_SNAPSHOTS);
        trimMap(faultDetectionMetricsMap, FAULT_DETECTION_SNAPSHOTS);
        trimMap(shardStateMetricsMap, SHARD_STATE_SNAPSHOTS);
        trimMap(gcInfoMap, GC_INFO_SNAPSHOTS);
        trimMap(clusterManagerThrottlingMetricsMap, CLUSTER_MANAGER_THROTTLING_SNAPSHOTS);
        trimMap(admissionControlMetricsMap, AC_SNAPSHOTS);

        for (NavigableMap<Long, MemoryDBSnapshot> snap : nodeMetricsMap.values()) {
            // do the same thing as OS_SNAPSHOTS.  Eventually MemoryDBSnapshot
            // will replace OSMetricsSnapshot as we want to our code to be
            // stable.
            trimMap(snap, OS_SNAPSHOTS);
        }
    }

    /**
     * Cleans up stale metricsdb files.
     *
     * @throws Exception if there is some problem closing the connection to a metricsdb file
     */
    public void trimOldMetricsDBFiles() throws Exception {
        boolean deleteDBFiles = PluginSettings.instance().shouldCleanupMetricsDBFiles();
        // Cleanup all but the 2 most recent metricsDB files from metricsDBMap. The most recent
        // metricsDB files needs to be
        // retained for future metrics query handling, the second most recent metricsDB file needs
        // to be retained in case
        // any metrics query handler just got access to it right before the most recent metricsDB
        // file was available.
        while (metricsDBMap.size() > MAX_DATABASES) {
            Map.Entry<Long, MetricsDB> oldestEntry = metricsDBMap.pollFirstEntry();
            if (oldestEntry != null) {
                Long key = oldestEntry.getKey();
                MetricsDB value = oldestEntry.getValue();
                value.remove();
                if (deleteDBFiles && !batchMetricsDBSet.contains(key)) {
                    value.deleteOnDiskFile();
                }
            }
        }
        // Flush any tracking batch metrics if batch metrics is disabled. Note, in order to ensure
        // that batch metrics
        // consumers have had at least one cycle to use any metrics they may be holding, this flush
        // is done before
        // re-reading the config file to update the state of the batch metrics feature.
        if (!batchMetricsEnabled && !batchMetricsDBSet.isEmpty()) {
            if (deleteDBFiles) {
                for (Long timestamp : batchMetricsDBSet) {
                    if (!metricsDBMap.containsKey(timestamp)) {
                        MetricsDB.deleteOnDiskFile(timestamp);
                    }
                }
            }
            batchMetricsDBSet.clear();
        }
        readBatchMetricsEnabledFromConf();
        // The (retentionPeriod * 12 + 2)'th database can be safely removed, since getBatchMetrics
        // never returns more than
        // the (retentionPeriod * 12) freshest metrics files. The (retentionPeriod * 12 + 1)'th file
        // is also retained in
        // case getBatchMetrics was called at the start of this cycle, right before the newest
        // metrics file was added to
        // the batchMetricsDBSet.
        long maxNumBatchMetricsDBFiles =
                PluginSettings.instance().getBatchMetricsRetentionPeriodMinutes() * 12 + 1;
        while (batchMetricsDBSet.size() > maxNumBatchMetricsDBFiles) {
            Long timestamp = batchMetricsDBSet.pollFirst();
            if (deleteDBFiles && !metricsDBMap.containsKey(timestamp)) {
                MetricsDB.deleteOnDiskFile(timestamp);
            }
        }
    }

    /** Deletes the lowest entries in the map till the size of the map is equal to maxSize. */
    private void trimMap(NavigableMap<Long, ?> map, int maxSize) throws Exception {
        // Remove the oldest entries from the map
        while (map.size() > maxSize) {
            Map.Entry<Long, ?> lowestEntry = map.firstEntry();
            if (lowestEntry != null) {
                Removable value = (Removable) lowestEntry.getValue();
                value.remove();
                map.remove(lowestEntry.getKey());
            }
        }
    }

    /**
     * Enrich event data with OS metrics and calculate aggregated metrics on dimensions like (shard,
     * index, operation, role). We emit metrics for the previous window interval as we need two
     * metric windows to align OSMetrics. Ex: To emit metrics between 5-10, we need OSMetrics
     * emitted at 8 and 13, to be able to calculate the metrics correctly. The aggregated metrics
     * are then written to a metricsDB.
     *
     * @param currWindowStartTime the start time of current sampling period. The bound of the period
     *     where that value is measured is MetricsConfiguration.SAMPLING_INTERVAL.
     * @throws Exception thrown if we have issues parsing metrics
     */
    private void emitMetrics(long currWindowStartTime) throws Exception {
        long prevWindowStartTime = currWindowStartTime - MetricsConfiguration.SAMPLING_INTERVAL;

        if (metricsDBMap.get(prevWindowStartTime) != null) {
            LOG.debug("The metrics for this timestamp already exist. Skipping.");
            return;
        }

        long mCurrT = System.currentTimeMillis();
        // This is object holds a reference to the temporary os snapshot. It is used to delete
        // tables at
        // the end of this
        // reader cycle. The OSMetricsSnapshot expects windowEndTime in the constructor.
        OSMetricsSnapshot alignedOSSnapHolder =
                new OSMetricsSnapshot(this.conn, "os_aligned_", currWindowStartTime);
        OSMetricsSnapshot osAlignedSnap =
                alignOSMetrics(
                        prevWindowStartTime,
                        prevWindowStartTime + MetricsConfiguration.SAMPLING_INTERVAL,
                        alignedOSSnapHolder);

        long mFinalT = System.currentTimeMillis();
        LOG.debug("Total time taken for aligning OS Metrics: {}", mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.READER_OS_METRICS_EMIT_TIME, (double) (mFinalT - mCurrT));

        mCurrT = System.currentTimeMillis();
        MetricsDB metricsDB = createMetricsDB(prevWindowStartTime);

        // Newly added metrics go at the bottom, do not change the ordering
        emitGarbageCollectionInfo(prevWindowStartTime, metricsDB);
        emitShardRequestMetrics(prevWindowStartTime, alignedOSSnapHolder, osAlignedSnap, metricsDB);
        emitHttpRequestMetrics(prevWindowStartTime, metricsDB);
        emitNodeMetrics(currWindowStartTime, metricsDB);
        emitShardStateMetrics(prevWindowStartTime, metricsDB);
        emitFaultDetectionMetrics(prevWindowStartTime, metricsDB);
        emitAdmissionControlMetrics(prevWindowStartTime, metricsDB);
        emitClusterManagerMetrics(prevWindowStartTime, metricsDB);
        emitClusterManagerThrottlingMetrics(prevWindowStartTime, metricsDB);

        metricsDB.commit();
        metricsDBMap.put(prevWindowStartTime, metricsDB);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.METRICSDB_FILE_SIZE, new File(metricsDB.getDBFilePath()).length());
        if (batchMetricsEnabled) {
            batchMetricsDBSet.add(prevWindowStartTime);
        }
        mFinalT = System.currentTimeMillis();
        LOG.debug("Total time taken for emitting Metrics: {}", mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.READER_METRICS_EMIT_TIME, (double) (mFinalT - mCurrT));
    }

    private void emitGarbageCollectionInfo(long prevWindowStartTime, MetricsDB metricsDB)
            throws Exception {
        if (gcInfoMap.containsKey(prevWindowStartTime)) {
            GarbageCollectorInfoSnapshot prevGcSnap = gcInfoMap.get(prevWindowStartTime);
            MetricsEmitter.emitGarbageCollectionInfo(metricsDB, prevGcSnap);
        } else {
            LOG.debug(
                    "Garbage collector information snapshot does not exist for the previous window. "
                            + "Not emitting metrics.");
        }
    }

    private void emitShardRequestMetrics(
            long prevWindowStartTime,
            OSMetricsSnapshot alignedOSSnapHolder,
            OSMetricsSnapshot osAlignedSnap,
            MetricsDB metricsDB)
            throws Exception {

        if (shardRqMetricsMap.containsKey(prevWindowStartTime)) {
            long mCurrT = System.currentTimeMillis();
            ShardRequestMetricsSnapshot preShardRequestMetricsSnapshot =
                    shardRqMetricsMap.get(prevWindowStartTime);
            LOG.debug(
                    "shard emit time {}, {}",
                    prevWindowStartTime,
                    preShardRequestMetricsSnapshot.windowStartTime);
            MetricsEmitter.emitWorkloadMetrics(
                    create, metricsDB, preShardRequestMetricsSnapshot); // calculate latency
            if (osAlignedSnap != null) {
                MetricsEmitter.emitAggregatedOSMetrics(
                        create,
                        metricsDB,
                        osAlignedSnap,
                        preShardRequestMetricsSnapshot); // table join
                MetricsEmitter.emitThreadNameMetrics(
                        create, metricsDB, osAlignedSnap); // threads other than bulk and query
            } else {
                LOG.debug("OS METRICS NULL");
            }
            alignedOSSnapHolder.remove();
            ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                    ReaderMetrics.SHARD_REQUEST_METRICS_EMITTER_EXECUTION_TIME,
                    System.currentTimeMillis() - mCurrT);
        } else {
            LOG.debug(
                    "Shard request snapshot for the previous window does not exist. Not emitting metrics.");
        }
    }

    private void emitHttpRequestMetrics(long prevWindowStartTime, MetricsDB metricsDB)
            throws Exception {

        if (httpRqMetricsMap.containsKey(prevWindowStartTime)) {

            HttpRequestMetricsSnapshot prevHttpRqSnap = httpRqMetricsMap.get(prevWindowStartTime);
            MetricsEmitter.emitHttpMetrics(create, metricsDB, prevHttpRqSnap);
        } else {
            LOG.debug(
                    "Http request snapshot for the previous window does not exist. Not emitting metrics.");
        }
    }

    /**
     * Enrich event data with node metrics and calculate aggregated metrics on dimensions like
     * (shard, index, operation, role). The aggregated metrics are then written to a metricsDB.
     *
     * @param currWindowStartTime the start time of current sampling period. The bound of the period
     *     where that value is measured is MetricsConfiguration.SAMPLING_INTERVAL.
     * @param metricsDB on-disk database to which we want to emit metrics
     * @throws Exception if we have issues emitting or aligning metrics
     */
    public void emitNodeMetrics(long currWindowStartTime, MetricsDB metricsDB) throws Exception {
        long prevWindowStartTime = currWindowStartTime - MetricsConfiguration.SAMPLING_INTERVAL;

        for (Map.Entry<AllMetrics.MetricName, NavigableMap<Long, MemoryDBSnapshot>> entry :
                nodeMetricsMap.entrySet()) {

            AllMetrics.MetricName metricName = entry.getKey();

            NavigableMap<Long, MemoryDBSnapshot> metricMap = entry.getValue();

            long mCurrT = System.currentTimeMillis();

            // This is object holds a reference to the temporary memory db
            // snapshot. It is used to delete tables at the end of this
            // reader cycle.

            MemoryDBSnapshot alignedSnapshotHolder =
                    new MemoryDBSnapshot(getConnection(), metricName, currWindowStartTime, true);
            MemoryDBSnapshot alignedSnapshot =
                    alignNodeMetrics(
                            metricName,
                            metricMap,
                            prevWindowStartTime,
                            currWindowStartTime,
                            alignedSnapshotHolder);

            long mFinalT = System.currentTimeMillis();
            LOG.debug("Total time taken for aligning {} Metrics: {}", metricName, mFinalT - mCurrT);

            if (alignedSnapshot == null) {
                alignedSnapshotHolder.remove();
                LOG.debug(
                        "{} snapshot for the previous window does not exist. Not emitting metrics.",
                        metricName);
                continue;
            }

            mCurrT = System.currentTimeMillis();
            MetricsEmitter.emitNodeMetrics(create, metricsDB, alignedSnapshot);

            // alignedSnapshotHolder cannot be the left or right window we are
            // trying to align, so we can safely remove.
            alignedSnapshotHolder.remove();

            mFinalT = System.currentTimeMillis();
            LOG.debug("Total time taken for emitting node metrics: {}", mFinalT - mCurrT);
        }
    }

    private void emitShardStateMetrics(long prevWindowStartTime, MetricsDB metricsDB) {
        if (shardStateMetricsMap.containsKey(prevWindowStartTime)) {
            ShardStateMetricsSnapshot prevShardsStateMetricsSnapshot =
                    shardStateMetricsMap.get(prevWindowStartTime);
            MetricsEmitter.emitShardStateMetric(metricsDB, prevShardsStateMetricsSnapshot);
        } else {
            LOG.debug(
                    "Shard State snapshot for the previous window does not exist. Not emitting metrics.");
        }
    }

    private void emitFaultDetectionMetrics(long prevWindowStartTime, MetricsDB metricsDB) {
        if (faultDetectionMetricsMap.containsKey(prevWindowStartTime)) {

            FaultDetectionMetricsSnapshot prevFaultDetectionSnap =
                    faultDetectionMetricsMap.get(prevWindowStartTime);
            MetricsEmitter.emitFaultDetectionMetrics(metricsDB, prevFaultDetectionSnap);
        } else {
            LOG.debug(
                    "Fault Detection snapshot for the previous window does not exist. Not emitting metrics.");
        }
    }

    private void emitAdmissionControlMetrics(long prevWindowStartTime, MetricsDB metricsDB)
            throws Exception {
        if (admissionControlMetricsMap.containsKey(prevWindowStartTime)) {
            AdmissionControlSnapshot previousSnapshot =
                    admissionControlMetricsMap.get(prevWindowStartTime);
            MetricsEmitter.emitAdmissionControlMetrics(metricsDB, previousSnapshot);
        } else {
            LOG.debug(
                    "Admission control snapshot does not exist for the previous window. Not emitting metrics.");
        }
    }

    private void emitClusterManagerMetrics(long prevWindowStartTime, MetricsDB metricsDB) {

        if (clusterManagerEventMetricsMap.containsKey(prevWindowStartTime)) {

            ClusterManagerEventMetricsSnapshot preClusterManagerEventSnapshot =
                    clusterManagerEventMetricsMap.get(prevWindowStartTime);
            MetricsEmitter.emitClusterManagerEventMetrics(
                    metricsDB, preClusterManagerEventSnapshot);
        } else {
            LOG.debug(
                    "ClusterManager snapshot for the previous window does not exist. Not emitting metrics.");
        }
    }

    private void emitClusterManagerThrottlingMetrics(
            long prevWindowStartTime, MetricsDB metricsDB) {
        if (clusterManagerThrottlingMetricsMap.containsKey(prevWindowStartTime)) {
            ClusterManagerThrottlingMetricsSnapshot prevShardsStateMetricsSnapshot =
                    clusterManagerThrottlingMetricsMap.get(prevWindowStartTime);
            MetricsEmitter.emitClusterManagerThrottledTaskMetric(
                    metricsDB, prevShardsStateMetricsSnapshot);
        } else {
            LOG.debug(
                    "ClusterManager Throttling snapshot for the previous window does not exist. Not emitting metrics.");
        }
    }

    /**
     * OS, Request, Http and cluster_manager first aligns the currentTimeStamp with a 5 second
     * interval. In the current format, a file (previously a directory) is written every 5 seconds.
     * So we actually read the last 5 second of the data.
     *
     * @param rootLocation Where to read the files from
     * @param currTimestamp The timestamp of the file that will be picked.
     * @throws Exception It can throw exception
     */
    public void processMetrics(String rootLocation, long currTimestamp) throws Exception {
        /*
         Querying a file by timestamp:
         1. Get the current system timestamp.
         2. Round it to the SAMPLING_TIME bucket. So, for a bucket width of
            5, a timestamp of 17 is dropped into the bucket numbered 15.
         3. Go, three windows back from the value you get in step 2. But you
            ask why ?
            The reason is the purger thread on the writer runs a sampling
            window behind the current timestamp. So for a current wall-clock
            time of 17 seconds, it will be purging events generated between
            wall clock time of 10 - 14 seconds. The purger does that
            because all collectors for the time window, 15 - 20 may not
            have run so far and we don't want to drop events on the wrong
            files. Now because the purger gathers the events in the window
            10 - 14 and writes them to a file named '10'.
            Now if the reader looks for the bucket 15, it will most certainly
            not find it, because it has not been created yet. If it looks
            for the bucket 10, it may or may not find it based on the fact
            if the purger has purged everything for the window of 10. There
            is a race here. The safest thing to do is to look for the
            window 5 because that is guaranteed to be be written by the
            purger at the moment, unless it has crashed and missed writing
            the file.
            However, there is a race condition here if let's say the writer is
            writing data at time 19.99 seconds. This write still falls into the
            bucket (10-15). At 20.01 the reader assumes that the bucket (10-15)
            is ready so it starts to read that file (go back two windows and
            fetch the file 10) But since writer just finished writing to
            the 10.tmp, it might not get enough to rotate that file before
            20.01. So race condition occurs. We have to add one additional window
            on reader to avoid this.
        */
        long mCurrT = System.currentTimeMillis();
        // Step 2 from above.
        long currWindowStartTime =
                PerformanceAnalyzerMetrics.getTimeInterval(
                        currTimestamp, MetricsConfiguration.SAMPLING_INTERVAL);

        // Step 3 from above.
        currWindowStartTime = currWindowStartTime - (3 * MetricsConfiguration.SAMPLING_INTERVAL);
        long currWindowEndTime = currWindowStartTime + MetricsConfiguration.SAMPLING_INTERVAL;

        EventProcessor osProcessor =
                OSEventProcessor.buildOSMetricEventsProcessor(
                        currWindowStartTime, currWindowEndTime, conn, osMetricsMap);
        EventProcessor requestProcessor =
                RequestEventProcessor.buildRequestMetricEventsProcessor(
                        currWindowStartTime, currWindowEndTime, conn, shardRqMetricsMap);
        EventProcessor httpProcessor =
                HttpRequestEventProcessor.buildHttpRequestMetricEventsProcessor(
                        currWindowStartTime, currWindowEndTime, conn, httpRqMetricsMap);
        EventProcessor faultDetectionProcessor =
                FaultDetectionMetricsProcessor.buildFaultDetectionMetricsProcessor(
                        currWindowStartTime, conn, faultDetectionMetricsMap);
        EventProcessor clusterManagerEventsProcessor =
                ClusterManagerMetricsEventProcessor.buildClusterManagerMetricEventsProcessor(
                        currWindowStartTime, conn, clusterManagerEventMetricsMap);
        EventProcessor nodeEventsProcessor =
                NodeMetricsEventProcessor.buildNodeMetricEventsProcessor(
                        currWindowStartTime, conn, nodeMetricsMap);
        EventProcessor shardStateMetricsProcessor =
                ShardStateMetricsProcessor.buildShardStateMetricEventsProcessor(
                        currWindowStartTime, conn, shardStateMetricsMap);
        EventProcessor garbageCollectorInfoProcessor =
                GarbageCollectorInfoProcessor.buildGarbageCollectorInfoProcessor(
                        currWindowStartTime, conn, gcInfoMap);
        EventProcessor clusterManagerThrottlingEventsProcessor =
                ClusterManagerThrottlingMetricsEventProcessor
                        .buildClusterManagerThrottlingMetricEventsProcessor(
                                currWindowStartTime, conn, clusterManagerThrottlingMetricsMap);
        ClusterDetailsEventProcessor clusterDetailsEventsProcessor =
                new ClusterDetailsEventProcessor(configOverridesApplier);
        EventProcessor admissionControlProcessor =
                AdmissionControlProcessor.build(
                        currWindowStartTime, conn, admissionControlMetricsMap);

        // The event dispatcher dispatches events to each of the registered event processors.
        // In addition to event processing each processor has an initialize/finalize function that
        // is
        // called
        // at the beginning and end of processing respectively.
        // We need to ensure that all the processors are registered, before the initialize function
        // is
        // called.
        // After all events have been processed, we call the finalizeProcessing function.
        EventDispatcher eventDispatcher = new EventDispatcher();

        eventDispatcher.registerEventProcessor(osProcessor);
        eventDispatcher.registerEventProcessor(requestProcessor);
        eventDispatcher.registerEventProcessor(httpProcessor);
        eventDispatcher.registerEventProcessor(nodeEventsProcessor);
        eventDispatcher.registerEventProcessor(clusterManagerEventsProcessor);
        eventDispatcher.registerEventProcessor(clusterManagerThrottlingEventsProcessor);
        eventDispatcher.registerEventProcessor(shardStateMetricsProcessor);
        eventDispatcher.registerEventProcessor(clusterDetailsEventsProcessor);
        eventDispatcher.registerEventProcessor(faultDetectionProcessor);
        eventDispatcher.registerEventProcessor(garbageCollectorInfoProcessor);
        eventDispatcher.registerEventProcessor(admissionControlProcessor);

        eventDispatcher.initializeProcessing(
                currWindowStartTime, currWindowStartTime + MetricsConfiguration.SAMPLING_INTERVAL);

        eventLogFileHandler.read(currWindowStartTime, eventDispatcher);

        eventDispatcher.finalizeProcessing();

        emitMetrics(currWindowStartTime);

        // There are cases, such as tests where appContext may not be initialized.
        // We always create a new ClusterDetailsEventsProcessor object above but we may not always
        // process the writer file, in which case the recently initialized
        // ClusterDetailsEventsProcessor does not contain valid values. Therefore, the empty check
        // for nodeDetails is required.
        if (appContext != null && !clusterDetailsEventsProcessor.getNodesDetails().isEmpty()) {
            appContext.setClusterDetailsEventProcessor(clusterDetailsEventsProcessor);
        }
        long mFinalT = System.currentTimeMillis();
        LOG.debug("Total time taken for processing Metrics: {}", mFinalT - mCurrT);
        ServiceMetrics.READER_METRICS_AGGREGATOR.updateStat(
                ReaderMetrics.READER_METRICS_PROCESS_TIME, (double) (mFinalT - mCurrT));
    }

    /**
     * Returns per thread OSMetrics between startTime and endTime. OSMetrics might have been
     * collected for windows that dont completely overlap with startTime and endTime. This function
     * calculates the weighted average of metrics in each overlapping window and sums them up to
     * find the average metrics in the requested window.
     *
     * @param startTime the start time of the previous sampling period. The bound of the period
     *     where that value is measured is MetricsConfiguration.SAMPLING_INTERVAL.
     * @param endTime the end time of the previous sampling period. The bound of the period where
     *     that value is measured is MetricsConfiguration.SAMPLING_INTERVAL.
     * @param alignedWindow where we store aligned snapshot
     * @return alignedWindow if we have two sampled snapshot; a sampled snapshot if we have only one
     *     sampled snapshot within startTime and endTime; null if the number of total snapshots is
     *     less than OS_SNAPSHOTS or if there is no snapshot taken after startTime or right window
     *     snapshot ends at or before endTime
     * @throws Exception thrown when we have issues in aligning window
     */
    public OSMetricsSnapshot alignOSMetrics(
            long startTime, long endTime, OSMetricsSnapshot alignedWindow) throws Exception {
        LOG.debug("Aligning metrics for {}, {}", startTime, endTime);
        // Find osmetric windows that overlap with the expected window.
        // This is atmost 2 but maybe less than 2. If less than 2, simply return the existing
        // window.

        // If we have insufficient snapshots just return
        if (osMetricsMap.size() < OS_SNAPSHOTS) {
            LOG.warn("Exited due to too few snapshots - {}", osMetricsMap.size());
            return null;
        }

        Map.Entry<Long, OSMetricsSnapshot> entry = osMetricsMap.higherEntry(startTime);
        // There is no snapshot taken after startTime.
        if (entry == null) {
            LOG.warn("No OS snapshot above startTime.");
            return null;
        }

        // Start time of the previous snapshot.
        Long t1 = entry.getKey();
        if (t1 == null) {
            LOG.error("We dont have an OS snapshot above startTime.");
            return null;
        }
        // Next higher key.
        Long t2 = osMetricsMap.higherKey(t1);

        if (t2 == null) {
            LOG.error("We dont have the next OS snapshot above startTime.");
            return entry.getValue();
        }

        if (t2 < endTime) {
            LOG.error(
                    "Right window snapshot ends before endTime. rw: {}, lw: {}, startTime: {}, endTime: {}",
                    t2,
                    t1,
                    startTime,
                    endTime);
            // TODO: As a quick fix we ignore this window. We might want to consider multiple
            // windows
            // instead.
            return null;
        }

        LOG.debug("Adding new scaled OS snapshot- actualTime {}", startTime);
        OSMetricsSnapshot leftWindow = osMetricsMap.get(t1);
        OSMetricsSnapshot rightWindow = osMetricsMap.get(t2);
        OSMetricsSnapshot.alignWindow(
                leftWindow, rightWindow, alignedWindow.getTableName(), startTime, endTime);
        return alignedWindow;
    }

    /**
     * Returns per node metrics between startTime and endTime. These metrics might have been
     * collected for windows that dont completely overlap with startTime and endTime. This function
     * calculates the weighted average of metrics in each overlapping window and sums them up to
     * find the average metrics in the requested window.
     *
     * <p>So in the code, startTime is "a" below, endTime is "b" below. Reader window is [a, b]. We
     * want to find "x", the cut-off point between two writer window.
     *
     * <p>Given metrics in two writer windows calculates a new reader window which overlaps with the
     * given windows. |------leftWindow-------|-------rightWindow--------| x a b
     * |-----------alignedWindow------|
     *
     * <p>We are emitting aligned metrics for previous window, not current window. This is to make
     * sure we have two windows to align. Otherwise, if we emit metrics for current window, we might
     * not have two writer window metrics.
     *
     * <p>If this is the time line:
     *
     * <p>+ writer writes to the left window at 2000l + reader reads at 6000l + writer writes to the
     * right window at 7000l + reader reads at 11000l Then according to
     * PerformanceAnalyzerMetrics.getTimeInterval, the previous reader window is [0, 5000], current
     * reader window is [5000, 10000].
     *
     * <p>If we align for current reader window, we need writer window ends in 7000l and 12000l. But
     * we don't have 12000l at 11000l.
     *
     * @param metricName the name of the metric we want to align
     * @param metricMap the in-memory database for this metric
     * @param readerStartTime the start time of the previous sampling period. The bound of the
     *     period where that value is measured is MetricsConfiguration.SAMPLING_INTERVAL.
     * @param readerEndTime the end time of the previous sampling period. The bound of the period
     *     where that value is measured is MetricsConfiguration.SAMPLING_INTERVAL.
     * @param alignedWindow where we store aligned snapshot
     * @return alignedWindow if we have two sampled snapshot; a sampled snapshot if we have only one
     *     sampled snapshot within startTime and endTime; null if the number of total snapshots is
     *     less than OS_SNAPSHOTS or if there is no snapshot taken after startTime or right window
     *     snapshot ends at or before endTime
     * @throws Exception thrown when we have issues in aligning window
     */
    public MemoryDBSnapshot alignNodeMetrics(
            AllMetrics.MetricName metricName,
            NavigableMap<Long, MemoryDBSnapshot> metricMap,
            long readerStartTime,
            long readerEndTime,
            MemoryDBSnapshot alignedWindow)
            throws Exception {

        LOG.debug(
                "Aligning node metrics for {}, from {} to {}",
                metricName,
                readerStartTime,
                readerEndTime);
        // Find metric windows that overlap with the expected window.
        // This is at most 2 but maybe less than 2. If less than 2, simply
        // return the existing window.

        // If we have insufficient snapshots just return
        // We need left writer window, right writer window. Also since we are
        // dealing with previous reader window, we need at least 3 snapshots.
        if (metricMap.size() < 3) {
            LOG.warn("Exited node metrics for {}, due to too few snapshots", metricName);
            return null;
        }

        // retrieve a snapshot ending at t1 = x
        Map.Entry<Long, MemoryDBSnapshot> entry = metricMap.ceilingEntry(readerStartTime);
        // There is no snapshot taken after startTime.
        if (entry == null) {
            LOG.warn("No {} metrics snapshot above startTime.", metricName);
            return null;
        }

        // Start time of the previous snapshot.
        Long t1 = entry.getKey();
        if (t1 == null) {
            LOG.error("We dont have an {} snapshot above startTime.", metricName);
            return null;
        }
        // Next higher key representing the end time of the rightWindow above

        Long t2 = metricMap.higherKey(t1);

        if (t2 == null) {
            LOG.error("We dont have the next {} snapshot above startTime.", metricName);
            return entry.getValue();
        }

        // t1 and startTime are already aligned. Just return the snapshot
        // between t2 and t1.
        if (t1 == readerStartTime) {
            LOG.debug("Found matching {} snapshot.", metricName);
            return metricMap.get(t2);
        }

        if (t2 <= readerEndTime) {
            LOG.error(
                    "Right window {} snapshot ends at or before endTime. rw: {}, lw: {}, startTime: {}, endTime: {}",
                    metricName,
                    t2,
                    t1,
                    readerStartTime,
                    readerEndTime);
            // TODO: As a quick fix we ignore this window. We might want to consider multiple
            // windows
            // instead.
            return null;
        }

        LOG.debug("Adding new scaled {} snapshot- actualTime {}", metricName, readerStartTime);
        // retrieve left and right window using osMetricsMap, whose key is the
        // largest last modification time.  We use values in the future to
        // represent values in the past.  So if at t1, writer writes values 1,
        // the interval [t1-sample interval, t1] has value 1.
        MemoryDBSnapshot leftWindow = metricMap.get(t1);
        MemoryDBSnapshot rightWindow = metricMap.get(t2);

        alignedWindow.alignWindow(leftWindow, rightWindow, t1, readerStartTime, readerEndTime);
        return alignedWindow;
    }

    public Connection getConnection() {
        return this.conn;
    }

    public DSLContext getDSLContext() {
        return this.create;
    }

    /**
     * This is called by operations outside of the ReaderMetricsProcessor.
     *
     * @return the latest on-disk database
     */
    public Map.Entry<Long, MetricsDB> getMetricsDB() {
        // If metricsDBMap is being trimmed we wait and acquire the latest
        return metricsDBMap.lastEntry();
    }

    public MetricsDB createMetricsDB(long timestamp) throws Exception {
        MetricsDB db = new MetricsDB(timestamp);
        return db;
    }

    public void deleteDBs() throws Exception {
        for (MetricsDB db : metricsDBMap.values()) {
            db.remove();
        }
    }

    /**
     * This is called by operations outside of the ReaderMetricsProcessor.
     *
     * @return A list of the timestamps associated with MetricsDB files. The oldest of the MetricsDB
     *     files typically have a lifetime of ~SAMPLING_INTERVAL seconds (no less than
     *     SAMPLING_INTERVAL/2 seconds). Null if batch metrics is disabled.
     */
    public NavigableSet<Long> getBatchMetrics() {
        if (batchMetricsEnabled) {
            TreeSet<Long> batchMetricsDBSetCopy = new TreeSet<>(batchMetricsDBSet.clone());
            long maxNumBatchMetricsDBFiles =
                    PluginSettings.instance().getBatchMetricsRetentionPeriodMinutes() * 12;
            while (batchMetricsDBSetCopy.size() > maxNumBatchMetricsDBFiles) {
                batchMetricsDBSetCopy.pollFirst();
            }
            return Collections.unmodifiableNavigableSet(batchMetricsDBSetCopy);
        }
        return null;
    }

    private void readBatchMetricsEnabledFromConf() {
        Path filePath = Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE);

        Util.invokePrivileged(
                () -> {
                    try (Scanner sc = new Scanner(filePath)) {
                        String nextLine = sc.nextLine();
                        boolean oldValue = batchMetricsEnabled;
                        boolean newValue = Boolean.parseBoolean(nextLine);
                        if (oldValue != newValue) {
                            batchMetricsEnabled = newValue;
                            LOG.info(
                                    "Batch metrics enabled changed from {} to {}",
                                    oldValue,
                                    newValue);
                        }
                    } catch (IOException e) {
                        StatsCollector.instance()
                                .logException(StatExceptionCode.BATCH_METRICS_CONFIG_ERROR);
                        LOG.error("Error reading file '{}': {}", filePath.toString(), e);
                        batchMetricsEnabled = defaultBatchMetricsEnabled;
                    }
                });
    }

    public boolean getBatchMetricsEnabled() {
        return batchMetricsEnabled;
    }

    /**
     * An example value is this: current_time:1566413987194 StartTime:1566413987194 ItemCount:359
     * IndexName:nyc_taxis ShardID:25 Primary:true Each pair is separated by new line and the key
     * and value within each pair is separated by ":" This function just parses the string and
     * generates the map as such.
     *
     * @param eventValue The value input to the helper function.
     * @return Returns a map of key value pairs
     */
    static Map<String, String> extractEntryData(String eventValue) {
        String[] lines = eventValue.split(System.lineSeparator());
        Map<String, String> keyValueMap = new HashMap<>();
        for (String line : lines) {
            String[] pair = line.split(PerformanceAnalyzerMetrics.sKeyValueDelimitor);
            if (pair.length == 1) {
                keyValueMap.put(pair[0], "");
            } else {
                keyValueMap.put(pair[0], pair[1]);
            }
        }
        return keyValueMap;
    }

    @VisibleForTesting
    Map<AllMetrics.MetricName, NavigableMap<Long, MemoryDBSnapshot>> getNodeMetricsMap() {
        return nodeMetricsMap;
    }

    @VisibleForTesting
    NavigableMap<Long, OSMetricsSnapshot> getOsMetricsMap() {
        return osMetricsMap;
    }

    @VisibleForTesting
    EventLogFileHandler getEventLogFileHandler() {
        return eventLogFileHandler;
    }

    @VisibleForTesting
    NavigableMap<Long, ShardRequestMetricsSnapshot> getShardRequestMetricsMap() {
        return shardRqMetricsMap;
    }

    @VisibleForTesting
    NavigableMap<Long, HttpRequestMetricsSnapshot> getHttpRqMetricsMap() {
        return httpRqMetricsMap;
    }

    @VisibleForTesting
    NavigableMap<Long, ClusterManagerEventMetricsSnapshot> getClusterManagerEventMetricsMap() {
        return clusterManagerEventMetricsMap;
    }

    @VisibleForTesting
    NavigableMap<Long, ClusterManagerThrottlingMetricsSnapshot>
            getClusterManagerThrottlingMetricsMap() {
        return clusterManagerThrottlingMetricsMap;
    }

    @VisibleForTesting
    NavigableMap<Long, ShardStateMetricsSnapshot> getShardStateMetricsMap() {
        return shardStateMetricsMap;
    }

    @VisibleForTesting
    void putNodeMetricsMap(
            AllMetrics.MetricName name, NavigableMap<Long, MemoryDBSnapshot> metricsMap) {
        this.nodeMetricsMap.put(name, metricsMap);
    }

    @VisibleForTesting
    NavigableMap<Long, MetricsDB> getMetricsDBMap() {
        return metricsDBMap;
    }

    @VisibleForTesting
    public void readBatchMetricsEnabledFromConfShim() {
        readBatchMetricsEnabledFromConf();
    }
}
