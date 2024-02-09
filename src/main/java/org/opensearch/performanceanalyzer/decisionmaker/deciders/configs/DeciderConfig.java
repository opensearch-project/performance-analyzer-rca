/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs;

import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.OldGenDecisionPolicyConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.young_gen.JvmGenTuningPolicyConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.NestedConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

/**
 * "decider-config-settings": { // Define the type of workload we can expect on the cluster. // User
 * can specify workload preference on ingest/search or skip this setting which implicitly // give
 * equal preference to ingest/search. "workload-type": { "prefer-ingest": true, "prefer-search":
 * false }, // Decreasing order of priority for the type of cache which is expected to be consumed
 * more. // Priority order in the list goes from most used to the lease used cache type.
 * "cache-type": { "priority-order": ["fielddata-cache", "shard-request-cache", "query-cache",
 * "bitset-filter-cache"] }, "old-gen-decision-policy-config": { XXXX },
 * "jvm-gen-tuning-policy-config": { XXXX }, },
 */
public class DeciderConfig {

    private static final String CACHE_CONFIG_NAME = "cache-type";
    private static final String WORKLOAD_CONFIG_NAME = "workload-type";
    private static final String OLD_GEN_DECISION_POLICY_CONFIG_NAME =
            "old-gen-decision-policy-config";
    private static final String JVM_GEN_TUNING_POLICY_CONFIG_NAME = "jvm-gen-tuning-policy-config";

    private final CachePriorityOrderConfig cachePriorityOrderConfig;
    private final WorkLoadTypeConfig workLoadTypeConfig;
    private final OldGenDecisionPolicyConfig oldGenDecisionPolicyConfig;
    private final JvmGenTuningPolicyConfig jvmGenTuningPolicyConfig;

    public DeciderConfig(final RcaConf rcaConf) {
        cachePriorityOrderConfig =
                new CachePriorityOrderConfig(
                        new NestedConfig(CACHE_CONFIG_NAME, rcaConf.getDeciderConfigSettings()));
        workLoadTypeConfig =
                new WorkLoadTypeConfig(
                        new NestedConfig(WORKLOAD_CONFIG_NAME, rcaConf.getDeciderConfigSettings()));
        oldGenDecisionPolicyConfig =
                new OldGenDecisionPolicyConfig(
                        new NestedConfig(
                                OLD_GEN_DECISION_POLICY_CONFIG_NAME,
                                rcaConf.getDeciderConfigSettings()));
        jvmGenTuningPolicyConfig =
                new JvmGenTuningPolicyConfig(
                        new NestedConfig(
                                JVM_GEN_TUNING_POLICY_CONFIG_NAME,
                                rcaConf.getDeciderConfigSettings()));
    }

    public CachePriorityOrderConfig getCachePriorityOrderConfig() {
        return cachePriorityOrderConfig;
    }

    public WorkLoadTypeConfig getWorkLoadTypeConfig() {
        return workLoadTypeConfig;
    }

    public OldGenDecisionPolicyConfig getOldGenDecisionPolicyConfig() {
        return oldGenDecisionPolicyConfig;
    }

    public JvmGenTuningPolicyConfig getJvmGenTuningPolicyConfig() {
        return jvmGenTuningPolicyConfig;
    }
}
