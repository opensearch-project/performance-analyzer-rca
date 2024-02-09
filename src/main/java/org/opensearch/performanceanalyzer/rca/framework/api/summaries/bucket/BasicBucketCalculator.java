/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.bucket;

import java.util.Map;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;

/**
 * BasicBucketCalculator is a {@link BucketCalculator} which places {@link Resource}s into {@link
 * UsageBucket}s based on defined ranges.
 */
public class BasicBucketCalculator implements BucketCalculator {
    // a value in (-inf, underUtilized] is considered underutilized and signals that additional
    // resources may be removed for the sake of frugality
    double underUtilized;
    // a value in (underutilized, healthyWithBuffer] is considered healthy, which means that we may
    // be able to increase the pressure on this resource
    double healthyWithBuffer;
    // a value in (healthyWithBuffer, healthy] is considered healthy and we probably shouldn't mess
    // with the resource
    // a value in (healthy, inf] is considered unhealthy and we should find ways to decrease the
    // pressure
    double healthy;

    public BasicBucketCalculator(double underUtilized, double healthyWithBuffer, double healthy) {
        this.underUtilized = underUtilized;
        this.healthyWithBuffer = healthyWithBuffer;
        this.healthy = healthy;
        if (underUtilized >= healthyWithBuffer || healthyWithBuffer >= healthy) {
            throw new IllegalArgumentException(
                    "BasicBucketCalculator must be instantiated with "
                            + "underUtilized < healthyWithBuffer < healthy but saw "
                            + this);
        }
    }

    public BasicBucketCalculator(final Map<UsageBucket, Double> bucketThresholdMap) {
        this(
                bucketThresholdMap.get(UsageBucket.UNDER_UTILIZED),
                bucketThresholdMap.get(UsageBucket.HEALTHY_WITH_BUFFER),
                bucketThresholdMap.get(UsageBucket.HEALTHY));
    }

    @Override
    public UsageBucket compute(ResourceEnum resource, double value) {
        return compute(value);
    }

    @Override
    public UsageBucket compute(double value) {
        if (value <= underUtilized) {
            return UsageBucket.UNDER_UTILIZED;
        } else if (value <= healthyWithBuffer) {
            return UsageBucket.HEALTHY_WITH_BUFFER;
        } else if (value <= healthy) {
            return UsageBucket.HEALTHY;
        } else {
            return UsageBucket.UNHEALTHY;
        }
    }

    @Override
    public String toString() {
        return "BasicBucketCalculator{"
                + "underUtilized="
                + underUtilized
                + ", healthyWithBuffer="
                + healthyWithBuffer
                + ", healthy="
                + healthy
                + '}';
    }
}
