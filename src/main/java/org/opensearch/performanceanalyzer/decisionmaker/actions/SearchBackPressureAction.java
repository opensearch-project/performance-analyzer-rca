/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class SearchBackPressureAction extends SuppressibleAction {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureAction.class);
    public static final String NAME = "SearchBackPressureAction";
    private static final String ID_KEY = "Id";
    private static final String IP_KEY = "Ip";

    /* placeholder for dummy impactVector
     * TODO: Remove
     */
    private static final ImpactVector NO_IMPACT = new ImpactVector();

    /* TO DO: Discuss the default cool off period for SearchBackPressureAction
     * Time to wait since last recommendation, before suggesting this action again
     */
    private static final long DEFAULT_COOL_OFF_PERIOD_IN_MILLIS = TimeUnit.DAYS.toMillis(3);

    // step size in percent
    /* From Config Per Diumension Type
     *  Dimension should include all the settings dimension (e.g. node_duress.cpu_threshold, search_heap_threshold)
     *  Step Size in percentage
     *  NOT to Node Level but for whole service (so all data node instances)
     *  canUpdate means whether the action should be emitted
     *
     */
    private final String searchbpDimension;
    private final double desiredValue;
    private final double currentValue;
    private boolean canUpdate;
    private long coolOffPeriodInMillis;

    public SearchBackPressureAction(
            final AppContext appContext,
            final boolean canUpdate,
            final long coolOffPeriodInMillis,
            final String searchbpDimension,
            final double desiredValue,
            final double currentValue) {
        super(appContext);
        this.canUpdate = canUpdate;
        this.coolOffPeriodInMillis = coolOffPeriodInMillis;
        this.searchbpDimension = searchbpDimension;
        this.desiredValue = desiredValue;
        this.currentValue = currentValue;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean canUpdate() {
        return canUpdate;
    }

    @Override
    public long coolOffPeriodInMillis() {
        return coolOffPeriodInMillis;
    }

    @Override
    public List<NodeKey> impactedNodes() {
        // all nodes are impacted by this change
        return appContext.getDataNodeInstances().stream()
                .map(NodeKey::new)
                .collect(Collectors.toList());
    }

    /* TO DO: Discuss the impact of SearchBackPressureAction  */
    @Override
    public Map<NodeKey, ImpactVector> impact() {
        Map<NodeKey, ImpactVector> impact = new HashMap<>();
        for (NodeKey key : impactedNodes()) {
            /* TODO: Impact Logic for SearchBackPressureAction */
            ImpactVector impactVector = new ImpactVector();
            impact.put(key, NO_IMPACT);
        }
        return impact;
    }

    public double getCurrentValue() {
        return this.currentValue;
    }

    public double getDesiredValue() {
        return this.desiredValue;
    }

    @Override
    public String summary() {
        Summary summary =
                new Summary(
                        searchbpDimension,
                        desiredValue,
                        currentValue,
                        DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
                        canUpdate);
        return summary.toJson();
    }

    /* Write Static Class Summary to conver the Action POJO to JSON Object
     * Key fields to be included
     * 1. Dimension (name) of the setting to be modified
     * 2. CurrentValue of the setting to be modified
     * 2. DesiredValue of the setting to be modified
     * 3. CoolOffPeriodInMillis
     * 4. canUpdate (whether the action should be emitted)
     */
    public static class Summary {
        public static final String SEARCHBP_SETTING_DIMENSION = "searchbp_setting_dimension";
        public static final String DESIRED_VALUE = "desiredValue";
        public static final String CURRENT_VALUE = "currentValue";
        public static final String COOL_OFF_PERIOD = "coolOffPeriodInMillis";
        public static final String CAN_UPDATE = "canUpdate";

        @SerializedName(value = SEARCHBP_SETTING_DIMENSION)
        private String searchbpSettingDimension;

        @SerializedName(value = DESIRED_VALUE)
        private double desiredValue;

        @SerializedName(value = CURRENT_VALUE)
        private double currentValue;

        @SerializedName(value = COOL_OFF_PERIOD)
        private long coolOffPeriodInMillis;

        @SerializedName(value = CAN_UPDATE)
        private boolean canUpdate;

        public Summary(
                String searchbpSettingDimension,
                double desiredValue,
                double currentValue,
                long coolOffPeriodInMillis,
                boolean canUpdate) {
            this.searchbpSettingDimension = searchbpSettingDimension;
            this.desiredValue = desiredValue;
            this.currentValue = currentValue;
            this.coolOffPeriodInMillis = coolOffPeriodInMillis;
            this.canUpdate = canUpdate;
        }

        public String getSearchbpSettingDimension() {
            return this.searchbpSettingDimension;
        }

        public double getCurrentValue() {
            return this.currentValue;
        }

        public double getDesiredValue() {
            return this.desiredValue;
        }

        public long getCoolOffPeriodInMillis() {
            return coolOffPeriodInMillis;
        }

        public boolean getCanUpdate() {
            return canUpdate;
        }

        public String toJson() {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            return gson.toJson(this);
        }
    }
}
