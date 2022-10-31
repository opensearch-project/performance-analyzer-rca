/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.ClusterType;

/**
 * This is a class level annotation that must be present for each of the RCAIt test classes. This
 * specifies the cluster type - single node vs multi-node with dedicated cluster_manager vs
 * multi-node with co-located cluster_manager.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AClusterType {
    ClusterType value();
}
