/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;

/**
 * This listener is notified whenever an action suggestion is published by the decision maker
 * Publisher
 */
public interface ActionListener {

    /** Called when Publisher emits an action */
    void actionPublished(Action action);
}
