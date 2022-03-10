/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;


import java.util.List;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;

/**
 * A DecisionPolicy evaluates a subset of observation summaries for a Decider, and returns a list of
 * recommended Actions. They abstract out a subset of the decision making process for a decider.
 *
 * <p>Decision policies are invoked by deciders and never scheduled directly by the RCA framework.
 */
public interface DecisionPolicy {

    List<Action> evaluate();
}
