/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.config.overrides;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.commons.config.overrides.ConfigOverrides;
import org.opensearch.performanceanalyzer.commons.config.overrides.ConfigOverridesHelper;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

/**
 * Class the manages applying the various overrides for RCAs, deciders, and action nodes as written
 * by the NodeDetails collector and read by the {@link ClusterDetailsEventProcessor}
 */
public class ConfigOverridesApplier {

    private static final Logger LOG = LogManager.getLogger(ConfigOverridesApplier.class);
    private long lastAppliedTimestamp;

    public void applyOverride(final String overridesJson, final String lastUpdatedTimestampString) {
        if (!valid(overridesJson, lastUpdatedTimestampString)) {
            LOG.warn(
                    "Received invalid overrides or timestamp. Overrides json: {}, last updated "
                            + "timestamp: {}",
                    overridesJson,
                    lastAppliedTimestamp);
            return;
        }

        try {
            long lastUpdatedTimestamp = Long.parseLong(lastUpdatedTimestampString);
            LOG.debug(
                    "Last updated(writer): {}, Last applied(reader): {}",
                    lastUpdatedTimestamp,
                    lastAppliedTimestamp);
            if (lastUpdatedTimestamp > lastAppliedTimestamp) {
                apply(ConfigOverridesHelper.deserialize(overridesJson));
            } else {
                LOG.debug(
                        "Not applying override. Last updated timestamp {} is behind last applied "
                                + "timestamp {}",
                        lastUpdatedTimestamp,
                        lastAppliedTimestamp);
            }
        } catch (IOException ioe) {
            LOG.error("Unable to deserialize overrides JSON:" + overridesJson, ioe);
        } catch (NumberFormatException nfe) {
            LOG.error(
                    "Unable to parse the lastUpdatedTimestamp {} string as a number.",
                    lastUpdatedTimestampString,
                    nfe);
        }
    }

    private void apply(final ConfigOverrides overrides) {
        if (PerformanceAnalyzerApp.getRcaController() != null
                && PerformanceAnalyzerApp.getRcaController().isRcaEnabled()) {
            LOG.info("Applying overrides: {}", overrides.getEnable().getRcas());
            RcaConf rcaConf = PerformanceAnalyzerApp.getRcaController().getRcaConf();
            if (rcaConf != null) {
                Set<String> currentMutedRcaSet = new HashSet<>(rcaConf.getMutedRcaList());
                Set<String> currentMutedDeciderSet = new HashSet<>(rcaConf.getMutedDeciderList());
                Set<String> currentMutedActionSet = new HashSet<>(rcaConf.getMutedActionList());
                // check and remove any nodes that are in the disabled list that were enabled just
                // now.
                if (overrides.getEnable() != null) {
                    if (overrides.getEnable().getRcas() != null) {
                        currentMutedRcaSet.removeAll(overrides.getEnable().getRcas());
                    }
                    if (overrides.getEnable().getDeciders() != null) {
                        currentMutedDeciderSet.removeAll(overrides.getEnable().getDeciders());
                    }
                    if (overrides.getEnable().getActions() != null) {
                        currentMutedActionSet.removeAll(overrides.getEnable().getActions());
                    }
                }

                // union the remaining already disabled nodes with the new set of disabled nodes.
                if (overrides.getDisable() != null) {
                    if (overrides.getDisable().getRcas() != null) {
                        currentMutedRcaSet.addAll(overrides.getDisable().getRcas());
                    }
                    if (overrides.getDisable().getDeciders() != null) {
                        currentMutedDeciderSet.addAll(overrides.getDisable().getDeciders());
                    }
                    if (overrides.getDisable().getActions() != null) {
                        currentMutedActionSet.addAll(overrides.getDisable().getActions());
                    }
                }

                LOG.info("New set of muted rcas: {}", currentMutedRcaSet);
                LOG.info("New set of muted deciders: {}", currentMutedDeciderSet);
                LOG.info("New set of muted actions: {}", currentMutedActionSet);
                boolean updateSuccess =
                        rcaConf.updateAllRcaConfFiles(
                                currentMutedRcaSet, currentMutedDeciderSet, currentMutedActionSet);
                if (updateSuccess) {
                    this.lastAppliedTimestamp = System.currentTimeMillis();
                }
            }
        }
    }

    private boolean valid(final String overridesJson, final String timestamp) {
        if (overridesJson == null || timestamp == null) {
            return false;
        }

        if (overridesJson.isEmpty() || timestamp.isEmpty()) {
            return false;
        }

        return StringUtils.isNumeric(timestamp);
    }

    @VisibleForTesting
    public long getLastAppliedTimestamp() {
        return lastAppliedTimestamp;
    }

    @VisibleForTesting
    public void setLastAppliedTimestamp(final long lastAppliedTimestamp) {
        this.lastAppliedTimestamp = lastAppliedTimestamp;
    }
}
