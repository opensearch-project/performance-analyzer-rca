/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.persistence;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;

/** A listener that persists all actions published by the publisher to rca.sqlite */
public class PublisherEventsPersistor {
    public static final String NAME = "publisher_events_persistor";
    private static final Logger LOG = LogManager.getLogger(PublisherEventsPersistor.class);

    private final Persistable persistable;

    public PublisherEventsPersistor(final Persistable persistable) {
        Objects.requireNonNull(persistable, "Persistable object cannot be null for:" + this.name());
        this.persistable = persistable;
    }

    public void persistAction(final List<Action> actionsPublished, long timestamp) {
        for (Action action : actionsPublished) {
            LOG.debug("Action: [{}] published to persistor publisher.", action.name());
            ServiceMetrics.RCA_RUNTIME_METRICS_AGGREGATOR.updateStat(
                    RcaRuntimeMetrics.ACTIONS_PUBLISHED, action.name(), 1);
            if (action.impactedNodes() != null) {
                final String nodeIds =
                        action.impactedNodes().stream()
                                .map(n -> n.getNodeId().toString())
                                .collect(Collectors.joining(",", "{", "}"));
                final String nodeIps =
                        action.impactedNodes().stream()
                                .map(n -> n.getHostAddress().toString())
                                .collect(Collectors.joining(",", "{", "}"));
                final PersistedAction actionsSummary = new PersistedAction();
                actionsSummary.setActionName(action.name());
                actionsSummary.setNodeIds(nodeIds);
                actionsSummary.setNodeIps(nodeIps);
                actionsSummary.setActionable(action.isActionable());
                actionsSummary.setCoolOffPeriod(action.coolOffPeriodInMillis());
                actionsSummary.setMuted(action.isMuted());
                actionsSummary.setSummary(action.summary());
                actionsSummary.setTimestamp(timestamp);
                try {
                    persistable.write(actionsSummary);
                } catch (Exception e) {
                    LOG.error("Unable to write publisher events to sqlite", e);
                    ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                            ExceptionsAndErrors.EXCEPTION_IN_PERSIST, action.name(), 1);
                }
            }
        }
    }

    public String name() {
        return NAME;
    }
}
