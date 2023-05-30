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
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.FieldDataCacheRcaConfig;
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
 * Field Data Cache RCA is to identify when the cache is unhealthy(thrashing) and otherwise,
 * healthy. The dimension we are using for this analysis is cache eviction count, cache current
 * weight(size) and cache max weight(size) configured. Note : For Field Data Cache, Hit and Miss
 * metrics aren't available.
 *
 * <p>Cache eviction within OpenSearch happens in following scenarios :
 *
 * <ol>
 *   <li>Mutation to Cache (Entry Insertion/Promotion and Manual Invalidation)
 *   <li>Explicit call to refresh()
 * </ol>
 *
 * <p>Cache Eviction requires either cache weight exceeds maximum weight OR the entry TTL is
 * expired. For Field Data Cache, no expire setting is present, so only in case of cache_weight
 * exceeding the max_cache_weight, eviction(removal from Cache Map and LRU linked List, entry
 * updated to EVICTED) happens.
 *
 * <p>Contrarily, the Cache Invalidation is performed manually on cache clear() and index close()
 * invocation, with removalReason as INVALIDATED and a force eviction is performed to ensure
 * cleanup.
 *
 * <p>This RCA reads 'fieldDataCacheEvictions', 'fieldDataCacheSizeGroupByOperation' and
 * 'fieldDataCacheMaxSizeGroupByOperation' from upstream metrics and maintains a collector which
 * keeps track of the time window period(tp) where we repeatedly see evictions for the last tp
 * duration. This RCA is marked as unhealthy if tp is above the threshold(300 seconds) and cache
 * size exceeds the max cache size configured.
 */
public class FieldDataCacheRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {
    private static final Logger LOG = LogManager.getLogger(FieldDataCacheRca.class);

    private final Metric fieldDataCacheEvictions;
    private final Metric fieldDataCacheSizeGroupByOperation;

    private final int rcaPeriod;
    private int counter;
    private double cacheSizeThreshold;
    protected Clock clock;
    private final CacheEvictionCollector cacheEvictionCollector;

    public <M extends Metric> FieldDataCacheRca(
            final int rcaPeriod,
            final M fieldDataCacheEvictions,
            final M fieldDataCacheSizeGroupByOperation) {
        super(5);
        this.rcaPeriod = rcaPeriod;
        this.fieldDataCacheEvictions = fieldDataCacheEvictions;
        this.fieldDataCacheSizeGroupByOperation = fieldDataCacheSizeGroupByOperation;
        this.counter = 0;
        this.cacheSizeThreshold = FieldDataCacheRcaConfig.DEFAULT_FIELD_DATA_CACHE_SIZE_THRESHOLD;
        this.clock = Clock.systemUTC();
        this.cacheEvictionCollector =
                new CacheEvictionCollector(
                        ResourceUtil.FIELD_DATA_CACHE_EVICTION,
                        fieldDataCacheEvictions,
                        FieldDataCacheRcaConfig.DEFAULT_FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC);
    }

    @VisibleForTesting
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        counter += 1;
        long currTimestamp = clock.millis();

        cacheEvictionCollector.collect(currTimestamp);
        if (counter >= rcaPeriod) {
            ResourceContext context;
            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

            double fieldDataCacheMaxSizeInBytes =
                    getCacheMaxSize(
                            getAppContext(),
                            new NodeKey(instanceDetails),
                            ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE);
            Boolean exceedsSizeThreshold =
                    isSizeThresholdExceeded(
                            fieldDataCacheSizeGroupByOperation,
                            fieldDataCacheMaxSizeInBytes,
                            cacheSizeThreshold);
            if (cacheEvictionCollector.isUnhealthy(currTimestamp) && exceedsSizeThreshold) {
                context = new ResourceContext(Resources.State.UNHEALTHY);
                nodeSummary.appendNestedSummary(
                        cacheEvictionCollector.generateSummary(currTimestamp));
                CommonStats.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                        RcaVerticesMetrics.NUM_FIELD_DATA_CACHE_RCA_TRIGGERED,
                        instanceDetails.getInstanceId().toString(),
                        1);
            } else {
                context = new ResourceContext(Resources.State.HEALTHY);
            }

            counter = 0;
            return new ResourceFlowUnit<>(
                    currTimestamp, context, nodeSummary, !instanceDetails.getIsClusterManager());
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
        FieldDataCacheRcaConfig configObj = conf.getFieldDataCacheRcaConfig();
        cacheSizeThreshold = configObj.getFieldDataCacheSizeThreshold();
        long cacheCollectorTimePeriodInSec =
                TimeUnit.SECONDS.toMillis(configObj.getFieldDataCollectorTimePeriodInSec());
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

    /** A collector class to collect eviction metrics */
    private static class CacheEvictionCollector {
        private final Resource cache;
        private final Metric cacheEvictionMetrics;
        private boolean hasEvictions;
        private long evictionTimestamp;
        private long metricTimePeriodInMillis;
        private int clearCounter;
        private int consecutivePeriodsToClear;

        private CacheEvictionCollector(
                final Resource cache,
                final Metric cacheEvictionMetrics,
                final int metricTimePeriodInMillis) {
            this.cache = cache;
            this.cacheEvictionMetrics = cacheEvictionMetrics;
            this.hasEvictions = false;
            this.evictionTimestamp = 0;
            this.metricTimePeriodInMillis = TimeUnit.SECONDS.toMillis(metricTimePeriodInMillis);
            this.clearCounter = 0;
            this.consecutivePeriodsToClear = 3;
        }

        public void setCollectorTimePeriod(long metricTimePeriodInMillis) {
            this.metricTimePeriodInMillis = metricTimePeriodInMillis;
        }

        public void collect(final long currTimestamp) {
            for (MetricFlowUnit flowUnit : cacheEvictionMetrics.getFlowUnits()) {
                if (flowUnit.isEmpty() || flowUnit.getData() == null) {
                    clearCounter += 1;
                    if (clearCounter > consecutivePeriodsToClear) {
                        // If the RCA receives 3 empty flow units, re-set the 'hasMetric' value
                        hasEvictions = false;
                        clearCounter = 0;
                        LOG.debug(
                                "{} encountered {} empty flow units, re-setting the 'hasEvictions value'.",
                                this.getClass().getSimpleName(),
                                consecutivePeriodsToClear);
                    }
                    CommonStats.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                            RcaGraphMetrics.RCA_RX_EMPTY_FU, this.getClass().getSimpleName(), 1);
                    continue;
                }

                double evictionCount =
                        flowUnit.getData().stream()
                                .mapToDouble(record -> record.getValue(MetricsDB.MAX, Double.class))
                                .sum();
                if (!Double.isNaN(evictionCount)) {
                    if (evictionCount > 0) {
                        if (!hasEvictions) {
                            evictionTimestamp = currTimestamp;
                        }
                        hasEvictions = true;
                    } else {
                        hasEvictions = false;
                    }
                } else {
                    LOG.error("Failed to parse metric from cache {}", cache.toString());
                }
            }
        }

        public boolean isUnhealthy(final long currTimestamp) {
            return hasEvictions && (currTimestamp - evictionTimestamp) >= metricTimePeriodInMillis;
        }

        private HotResourceSummary generateSummary(final long currTimestamp) {
            return new HotResourceSummary(
                    cache,
                    TimeUnit.MILLISECONDS.toSeconds(metricTimePeriodInMillis),
                    TimeUnit.MILLISECONDS.toSeconds(currTimestamp - evictionTimestamp),
                    0);
        }
    }
}
