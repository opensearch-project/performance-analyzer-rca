/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails.Id;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails.Ip;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.apache.logging.log4j.LogManager;

public class SearchBackPressureAction extends SuppressibleAction {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureAction.class);
    public static final String NAME = "SearchBackPressureAction";
    private static final String ID_KEY = "Id";
    private static final String IP_KEY = "Ip";
    private final NodeKey node; 

    /* TO DO: Discuss the default cool off period for SearchBackPressureAction 
     * Time to wait since last recommendation, before suggesting this action again 
     */
    private static final long DEFAULT_COOL_OFF_PERIOD_IN_MILLIS = TimeUnit.DAYS.toMillis(3);

    // step size in percent 


}
    
