/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.bucket;


import org.opensearch.performanceanalyzer.grpc.Resource;

/**
 * A UsageBucket is associated with a {@link Resource} and identifies the state of that Resource. We
 * use these buckets to identify when we have the bandwidth to scale a particular resource out or
 * in.
 *
 * <p>{@link UsageBucket#UNDER_UTILIZED} means that the {@link Resource} is barely being used at all
 * and may be a good candidate for scaling in.
 *
 * <p>{@link UsageBucket#HEALTHY_WITH_BUFFER} means that the {@link Resource} is healthy and there
 * is room to increase the pressure on this resource if need be.
 *
 * <p>{@link UsageBucket#HEALTHY} means that the {@link Resource} is in a healthy state. Resources
 * in this bucket should probably be left alone.
 *
 * <p>{@link UsageBucket#UNHEALTHY} means that the {@link Resource} is under high pressure. Actions
 * should be taken to help reduce the pressure.
 */
public enum UsageBucket {
    UNKNOWN,
    UNDER_UTILIZED,
    HEALTHY_WITH_BUFFER,
    HEALTHY,
    UNHEALTHY
}
