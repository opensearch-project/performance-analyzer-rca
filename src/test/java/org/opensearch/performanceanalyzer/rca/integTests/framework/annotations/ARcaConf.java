/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.Consts;

/**
 * This annotation can be used to specify an rca.conf file. Usually tests don't need to provide the
 * rca.conf therefore, it uses the rca.conf* files in the test/resources as defaults.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ARcaConf {
    // full path to the rca.conf file to be used by elected cluster_manager node.
    String electedClusterManager() default
            Consts.RCAIT_DEFAULT_RCA_CONF_ELECTED_CLUSTER_MANAGER_NODE;

    // full path to the rca.conf file to be used by the standby cluster_manager.
    String standBy() default Consts.RCAIT_DEFAULT_RCA_CONF_STANDBY_CLUSTER_MANAGER_NODE;

    // full path to the rca.conf file to be used by the data node.
    String dataNode() default Consts.RCAIT_DEFAULT_RCA_CONF_DATA_NODE;

    enum Type {
        ELECTED_CLUSTER_MANAGER,
        STANDBY_CLUSTER_MANAGER,
        DATA_NODES
    }
}
