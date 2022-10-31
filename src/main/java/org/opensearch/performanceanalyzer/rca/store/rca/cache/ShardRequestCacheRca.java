/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.cache;

import static org.opensearch.performanceanalyzer.rca.store.rca.cache.CacheUtil.getCacheMaxSize;
import static org.opensearch.performanceanalyzer.rca.store.rca.cache.CacheUtil.isSizeThresholdExceeded;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.ShardRequestCacheRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaVerticesMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

/**
 * Shard Request Cache RCA is to identify when the cache is unhealthy(thrashing) and otherwise,
 * healthy. The dimension we are using for this analysis is cache eviction, hit count, cache current
 * weight(size) and cache max weight(size) configured.
 *
 * <p>Cache eviction within OpenSearch happens in following scenarios:
 *
 * <ol>
 *   <li>Mutation to Cache (Entry Insertion/Promotion and Manual Invalidation)
 *   <li>Explicit call to refresh()
 * </ol>
 *
 * <p>Cache Eviction requires either cache weight exceeds maximum weight OR the entry TTL is
 * expired. For Shard Request Cache, TTL is defined via `indices.requests.cache.expire` setting
 * which is never used in production clusters and only provided for backward compatibility, thus we
 * ignore time based evictions. The weight based evictions(removal from Cache Map and LRU linked
 * List with entry updated to EVICTED) occur when the cache_weight exceeds the max_cache_weight,
 * eviction.
 *
 * <p>The Entry Invalidation is performed manually on cache clear(), index close() and for cached
 * results from timed-out requests. A scheduled runnable, running every 10 minutes cleans up all the
 * invalidated entries which have not been read/written to since invalidation.
 *
 * <p>The Cache Hit and Eviction metric presence implies cache is undergoing frequent load and
 * eviction or undergoing scheduled cleanup for entries which had timed-out during execution.
 *
 * <p>This RCA reads 'shardRequestCacheEvictions', 'shardRequestCacheHits',
 * 'shardRequestCacheSizeGroupByOperation' and 'shardRequestCacheMaxSizeInBytes' from upstream
 * metrics and maintains collectors which keep track of time window period(tp) where we repeatedly
 * see evictions and hits for the last tp duration. This RCA is marked as unhealthy if tp we find tp
 * is above the threshold(300 seconds) and cache size exceeds the max cache size configured.
 */
public class ShardRequestCacheRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {
    private static final Logger LOG = LogManager.getLogger(ShardRequestCacheRca.class);

    private final Metric shardRequestCacheEvictions;
    private final Metric shardRequestCacheHits;
    private final Metric shardRequestCacheSizeGroupByOperation;
    private final int rcaPeriod;
    private int counter;
    private double cacheSizeThreshold;
    protected Clock clock;
    private final CacheCollector cacheEvictionCollector;
    private final CacheCollector cacheHitCollector;

    public <M extends Metric> ShardRequestCacheRca(
            final int rcaPeriod,
            final M shardRequestCacheEvictions,
            final M shardRequestCacheHits,
            final M shardRequestCacheSizeGroupByOperation) {
        super(5);
        this.rcaPeriod = rcaPeriod;
        this.shardRequestCacheEvictions = shardRequestCacheEvictions;
        this.shardRequestCacheHits = shardRequestCacheHits;
        this.shardRequestCacheSizeGroupByOperation = shardRequestCacheSizeGroupByOperation;
        this.counter = 0;
        this.cacheSizeThreshold =
                ShardRequestCacheRcaConfig.DEFAULT_SHARD_REQUEST_CACHE_SIZE_THRESHOLD;
        this.clock = Clock.systemUTC();
        this.cacheEvictionCollector =
                new CacheCollector(
                        ResourceUtil.SHARD_REQUEST_CACHE_EVICTION,
                        shardRequestCacheEvictions,
                        ShardRequestCacheRcaConfig
                                .DEFAULT_SHARD_REQUEST_COLLECTOR_TIME_PERIOD_IN_SEC);
        this.cacheHitCollector =
                new CacheCollector(
                        ResourceUtil.SHARD_REQUEST_CACHE_HIT,
                        shardRequestCacheHits,
                        ShardRequestCacheRcaConfig
                                .DEFAULT_SHARD_REQUEST_COLLECTOR_TIME_PERIOD_IN_SEC);
    }

    @VisibleForTesting
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ResourceFlowUnit operate() {
        counter += 1;
        long currTimestamp = clock.millis();

        cacheEvictionCollector.collect(currTimestamp);
        cacheHitCollector.collect(currTimestamp);
        if (counter >= rcaPeriod) {
            ResourceContext context;
            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

            double shardRequestCacheMaxSizeInBytes =
                    getCacheMaxSize(
                            getAppContext(),
                            new NodeKey(instanceDetails),
                            ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE);
            Boolean exceedsSizeThreshold =
                    isSizeThresholdExceeded(
                            shardRequestCacheSizeGroupByOperation,
                            shardRequestCacheMaxSizeInBytes,
                            cacheSizeThreshold);

            // if eviction and hit counts persists in last 5 minutes and cache size exceeds max
            // cache size * threshold percentage,
            // the cache is considered as unhealthy
            if (cacheEvictionCollector.isUnhealthy(currTimestamp)
                    && cacheHitCollector.isUnhealthy(currTimestamp)
                    && exceedsSizeThreshold) {
                context = new ResourceContext(Resources.State.UNHEALTHY);
                nodeSummary.appendNestedSummary(
                        cacheEvictionCollector.generateSummary(currTimestamp));
                PerformanceAnalyzerApp.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                        RcaVerticesMetrics.NUM_SHARD_REQUEST_CACHE_RCA_TRIGGERED,
                        instanceDetails.getInstanceId().toString(),
                        1);
            } else {
                context = new ResourceContext(Resources.State.HEALTHY);
            }

            counter = 0;
            return new ResourceFlowUnit<>(
                    currTimestamp, context, nodeSummary, !instanceDetails.getIsMaster());
        } else {
            return new ResourceFlowUnit<>(currTimestamp);
        }
    }

    /**
     * read threshold values from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        ShardRequestCacheRcaConfig configObj = conf.getShardRequestCacheRcaConfig();
        cacheSizeThreshold = configObj.getShardRequestCacheSizeThreshold();
        long cacheCollectorTimePeriodInSec =
                TimeUnit.SECONDS.toMillis(configObj.getShardRequestCollectorTimePeriodInSec());
        cacheHitCollector.setCollectorTimePeriod(cacheCollectorTimePeriodInSec);
        cacheEvictionCollector.setCollectorTimePeriod(cacheCollectorTimePeriodInSec);
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        List<ResourceFlowUnit<HotNodeSummary>> flowUnitList = new ArrayList<>();
        LOG.debug("rca: Executing fromWire: {}", this.getClass().getSimpleName());
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }

    /** A collector class to collect metrics (eviction and hit) for cache */
    private static class CacheCollector {
        private final Resource cache;
        private final Metric cacheMetrics;
        private boolean hasCacheMetric;
        private long metricTimestamp;
        private long metricTimePeriodInMillis;
        private int clearCounter;
        private int consecutivePeriodsToClear;

        public CacheCollector(
                final Resource cache, final Metric cacheMetrics, final int metricTimePeriodInSec) {
            this.cache = cache;
            this.cacheMetrics = cacheMetrics;
            this.hasCacheMetric = false;
            this.metricTimestamp = 0;
            this.metricTimePeriodInMillis = TimeUnit.SECONDS.toMillis(metricTimePeriodInSec);
            this.clearCounter = 0;
            this.consecutivePeriodsToClear = 3;
        }

        public void setCollectorTimePeriod(long metricTimePeriodInMillis) {
            this.metricTimePeriodInMillis = metricTimePeriodInMillis;
        }

        public void collect(final long currTimestamp) {
            for (MetricFlowUnit flowUnit : cacheMetrics.getFlowUnits()) {
                if (flowUnit.isEmpty()) {
                    clearCounter += 1;
                    if (clearCounter > consecutivePeriodsToClear) {
                        // If the RCA receives 3 empty flow units, re-set the 'hasCacheMetric' value
                        hasCacheMetric = false;
                        clearCounter = 0;
                        LOG.debug(
                                "{} encountered {} empty flow units, re-setting the hasCacheMetric value.",
                                this.getClass().getSimpleName(),
                                consecutivePeriodsToClear);
                    }
                    PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                            RcaGraphMetrics.RCA_RX_EMPTY_FU, this.getClass().getSimpleName(), 1);
                    continue;
                }

                Result<Record> records = flowUnit.getData();
                double metricCount =
                        records.stream()
                                .mapToDouble(record -> record.getValue(MetricsDB.MAX, Double.class))
                                .sum();
                if (!Double.isNaN(metricCount)) {
                    if (metricCount > 0) {
                        if (!hasCacheMetric) {
                            metricTimestamp = currTimestamp;
                        }
                        hasCacheMetric = true;
                    } else {
                        hasCacheMetric = false;
                    }
                } else {
                    LOG.error("Failed to parse metric from cache {}", cache.toString());
                }
            }
        }

        public boolean isUnhealthy(final long currTimestamp) {
            return hasCacheMetric && (currTimestamp - metricTimestamp) >= metricTimePeriodInMillis;
        }

        private HotResourceSummary generateSummary(final long currTimestamp) {
            return new HotResourceSummary(
                    cache,
                    TimeUnit.MILLISECONDS.toSeconds(metricTimePeriodInMillis),
                    TimeUnit.MILLISECONDS.toSeconds(currTimestamp - metricTimestamp),
                    0);
        }
    }
}
