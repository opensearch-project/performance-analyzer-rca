/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.SearchBackPressureAction;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.Decider;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.Decision;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure.SearchBackPressureClusterRCA;

/** decider to change the dynamic settings of SearchBackPressure In-flight Cancellation */
public class SearchBackPressureDecider extends Decider {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureDecider.class);
    public static final String NAME = "SearchBackPressureDecider";

    private final SearchBackPressurePolicy searchBackPressurePolicy;

    private int currentIteration = 0;
    private SearchBackPressureClusterRCA searchBackPressureClusterRCA;

    public SearchBackPressureDecider(
            long evalIntervalSeconds,
            int decisionFrequency,
            SearchBackPressureClusterRCA searchBackPressureClusterRCA) {
        super(evalIntervalSeconds, decisionFrequency);
        this.searchBackPressureClusterRCA = searchBackPressureClusterRCA;
        this.searchBackPressurePolicy = new SearchBackPressurePolicy(searchBackPressureClusterRCA);
        LOG.debug("SearchBackPressureDecider created");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Decision operate() {
        LOG.debug(
                "SearchBackPressureDecider#2 operate() with currentIteration: {}",
                currentIteration);

        Decision decision = new Decision(System.currentTimeMillis(), NAME);
        currentIteration += 1;
        if (currentIteration < decisionFrequency) {
            return decision;
        }

        // reset the currentIteration for next action emitting cycle
        currentIteration = 0;

        // SearchBackPressure Policy is always accepted since Searchbp Decider only use the actions
        // suggested by Searchbp Policy
        List<Action> searchBackPressureActions = searchBackPressurePolicy.evaluate();

        // loop through the actions and print the action threshold name, dimension,
        // increase/decrease
        searchBackPressureActions.stream()
                .forEach(
                        (action) -> {
                            LOG.debug(
                                    "searchBackPressureActions details, threshold name: {}, dimension: {}, increase/decrease: {}, stepsize: {}",
                                    ((SearchBackPressureAction) action).getThresholdName(),
                                    ((SearchBackPressureAction) action).getDimension(),
                                    ((SearchBackPressureAction) action).getDirection(),
                                    ((SearchBackPressureAction) action).getStepSizeInPercentage());
                        });

        searchBackPressureActions.forEach(decision::addAction);

        LOG.debug("decision action size is {}", decision.getActions().size());
        return decision;
    }

    /* Read RCA Config to fill the dynamic threshold settings for the SearchBackPressure Service */
    @Override
    public void readRcaConf(RcaConf conf) {
        super.readRcaConf(conf);
        searchBackPressurePolicy.setRcaConf(conf);
    }

    /* Set AppContext for SearchBackPressurePolicy */
    @Override
    public void setAppContext(final AppContext appContext) {
        super.setAppContext(appContext);
        searchBackPressurePolicy.setAppContext(appContext);
    }
}
