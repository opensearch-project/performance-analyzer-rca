/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.plugins;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ActionListener;

/** A simple listener that logs all actions published by the publisher */
public class PublisherEventsLogger extends Plugin implements ActionListener {

    private static final Logger LOG = LogManager.getLogger(PublisherEventsLogger.class);
    public static final String NAME = "publisher_events_logger_plugin";

    @Override
    public void actionPublished(Action action) {
        LOG.info(
                "Action: [{}] published by decision maker publisher. action summary : {}",
                action.name(),
                action.summary());
    }

    @Override
    public String name() {
        return NAME;
    }
}
