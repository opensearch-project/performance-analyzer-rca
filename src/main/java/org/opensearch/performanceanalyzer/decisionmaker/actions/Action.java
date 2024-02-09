/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;

import java.util.List;
import java.util.Map;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public interface Action {

    /**
     * Returns true if the configured action is actionable, false otherwise.
     *
     * <p>Examples of non-actionable actions are resource configurations where limits have been
     * reached.
     */
    boolean isActionable();

    /** Time to wait since last recommendation, before suggesting this action again */
    long coolOffPeriodInMillis();

    /** Returns a list of nodes impacted by this action. */
    List<NodeKey> impactedNodes();

    /** Returns a map of nodes to ImpactVector of this action on that node */
    Map<NodeKey, ImpactVector> impact();

    /** Returns action name */
    String name();

    /** Returns a summary for the configured action */
    String summary();

    /** Returns if this action is explicitly muted through configuration */
    boolean isMuted();
}
