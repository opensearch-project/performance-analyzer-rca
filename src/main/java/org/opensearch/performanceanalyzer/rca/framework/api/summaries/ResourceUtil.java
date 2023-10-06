/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries;


import org.opensearch.performanceanalyzer.grpc.AdditionalFields;
import org.opensearch.performanceanalyzer.grpc.MetricEnum;
import org.opensearch.performanceanalyzer.grpc.PANetworking;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;

/**
 * A utility class to parse and build grpc Resource message Resource message consist of two parts :
 * ResourceEnum and MetricEnum. Both Enum types are defined in protobuf
 * (src/main/proto/inter_node_rpc_service.proto)
 *
 * <p>ResourceEnum : different resource type on instance : CPU, IO, CACHE, etc. MetricEnum : metrics
 * of each resource type, i.eZ. IO can have metrics such as TOTAL_THROUGHPUT and SYS_CALL_RATE
 */
public class ResourceUtil {
    // JVM resources
    public static final Resource HEAP_MAX_SIZE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.HEAP)
                    .setMetricEnum(MetricEnum.HEAP_MAX)
                    .build();

    public static final Resource HEAP_ALLOC_RATE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.HEAP)
                    .setMetricEnum(MetricEnum.HEAP_ALLOC_RATE)
                    .build();

    public static final Resource OLD_GEN_HEAP_USAGE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.OLD_GEN)
                    .setMetricEnum(MetricEnum.HEAP_USAGE)
                    .build();
    public static final Resource YOUNG_GEN_PROMOTION_RATE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.YOUNG_GEN)
                    .setMetricEnum(MetricEnum.PROMOTION_RATE)
                    .build();
    public static final Resource FULL_GC_EFFECTIVENESS =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.OLD_GEN)
                    .setMetricEnum(MetricEnum.OLD_GEN_USAGE_AFTER_FULL_GC)
                    .build();
    public static final Resource FULL_GC_PAUSE_TIME =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.OLD_GEN)
                    .setMetricEnum(MetricEnum.FULL_GC)
                    .build();
    public static final Resource OLD_GEN_MAX_SIZE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.OLD_GEN)
                    .setMetricEnum(MetricEnum.HEAP_MAX)
                    .build();
    public static final Resource YOUNG_GEN_MAX_SIZE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.YOUNG_GEN)
                    .setMetricEnum(MetricEnum.HEAP_MAX)
                    .build();
    public static final Resource MINOR_GC_PAUSE_TIME =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.YOUNG_GEN)
                    .setMetricEnum(MetricEnum.MINOR_GC)
                    .build();

    // hardware resource
    public static final Resource CPU_USAGE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.CPU)
                    .setMetricEnum(MetricEnum.CPU_USAGE)
                    .build();
    public static final Resource IO_TOTAL_THROUGHPUT =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.IO)
                    .setMetricEnum(MetricEnum.TOTAL_THROUGHPUT)
                    .build();
    public static final Resource IO_TOTAL_SYS_CALLRATE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.IO)
                    .setMetricEnum(MetricEnum.TOTAL_SYS_CALLRATE)
                    .build();

    // thread pool
    public static final Resource WRITE_QUEUE_REJECTION =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.WRITE_THREADPOOL)
                    .setMetricEnum(MetricEnum.QUEUE_REJECTION)
                    .build();
    public static final Resource SEARCH_QUEUE_REJECTION =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.SEARCH_THREADPOOL)
                    .setMetricEnum(MetricEnum.QUEUE_REJECTION)
                    .build();
    public static final Resource WRITE_QUEUE_CAPACITY =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.WRITE_THREADPOOL)
                    .setMetricEnum(MetricEnum.QUEUE_CAPACITY)
                    .build();
    public static final Resource SEARCH_QUEUE_CAPACITY =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.SEARCH_THREADPOOL)
                    .setMetricEnum(MetricEnum.QUEUE_CAPACITY)
                    .build();

    // cache
    public static final Resource FIELD_DATA_CACHE_EVICTION =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.FIELD_DATA_CACHE)
                    .setMetricEnum(MetricEnum.CACHE_EVICTION)
                    .build();
    public static final Resource FIELD_DATA_CACHE_MAX_SIZE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.FIELD_DATA_CACHE)
                    .setMetricEnum(MetricEnum.CACHE_MAX_SIZE)
                    .build();
    public static final Resource SHARD_REQUEST_CACHE_EVICTION =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.SHARD_REQUEST_CACHE)
                    .setMetricEnum(MetricEnum.CACHE_EVICTION)
                    .build();
    public static final Resource SHARD_REQUEST_CACHE_HIT =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.SHARD_REQUEST_CACHE)
                    .setMetricEnum(MetricEnum.CACHE_HIT)
                    .build();
    public static final Resource SHARD_REQUEST_CACHE_MAX_SIZE =
            Resource.newBuilder()
                    .setResourceEnum(ResourceEnum.SHARD_REQUEST_CACHE)
                    .setMetricEnum(MetricEnum.CACHE_MAX_SIZE)
                    .build();

    /**
     * Read the resourceType name from the ResourceType object
     *
     * @param resource grpc Resource object
     * @return resource type name
     */
    public static String getResourceTypeName(Resource resource) {
        return resource.getResourceEnum()
                .getValueDescriptor()
                .getOptions()
                .getExtension(PANetworking.additionalFields)
                .getName();
    }

    /**
     * Read the resourceType unit type from the ResourceType object
     *
     * @param resource grpc ResourceType object
     * @return resource unit type
     */
    public static String getResourceMetricName(Resource resource) {
        AdditionalFields resourceMetricOptions =
                resource.getMetricEnum()
                        .getValueDescriptor()
                        .getOptions()
                        .getExtension(PANetworking.additionalFields);
        return resourceMetricOptions.getName() + "(" + resourceMetricOptions.getDescription() + ")";
    }

    /**
     * Build Resource object from enum value
     *
     * @param resourceEnumValue resource enum value
     * @param metricEnumValue metric enum value
     * @return ResourceType enum object
     */
    public static Resource buildResource(int resourceEnumValue, int metricEnumValue) {
        Resource.Builder builder = Resource.newBuilder();
        builder.setResourceEnumValue(resourceEnumValue);
        builder.setMetricEnumValue(metricEnumValue);
        return builder.build();
    }
}
