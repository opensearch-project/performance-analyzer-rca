/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.bucket;


import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;

/**
 * A BucketCalculator identifies which {@link UsageBucket} a {@link Resource} should be placed into
 * given that resource's value. It does this on a per-node basis.
 *
 * <p>E.g. a BucketCalculator can compute a value of HEALTHY for CPU on Node "A" and a value of
 * HEALTHY_WITH_BUFFER for CPU on Node "B". A consumer of this information can then read out these
 * bucket values by calling something like getUsageBucket(NodeA, CPU).
 */
public interface BucketCalculator {
    /**
     * Identifies which {@link UsageBucket} a {@link Resource} should be placed in given that
     * resource's value.
     *
     * @param resource The resource to check
     * @param value The metric value of the resource (this may be a percentage, duration, etc.) it's
     *     up to the implementation how to handle this value for a particular {@link Resource}
     * @return The {@link UsageBucket} that the {@link Resource} should be associated with
     */
    UsageBucket compute(ResourceEnum resource, double value);

    /**
     * Given value, try to find a bucket for it.
     *
     * @param value The double value we want to find a bucket for.
     * @return The Bucket that can fit this value.
     */
    UsageBucket compute(double value);
}
