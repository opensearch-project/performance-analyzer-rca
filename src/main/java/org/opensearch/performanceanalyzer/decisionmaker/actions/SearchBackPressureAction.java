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
    private static final ImpactVector NO_IMPACT = new ImpactVector();

    /*
     * Time to wait since last recommendation, before suggesting this action again
     */
    private static final long DEFAULT_COOL_OFF_PERIOD_IN_MILLIS = TimeUnit.DAYS.toMillis(1);

    /* From Config Per Diumension Type
     *  canUpdate: whether the action should be emitted
     *  coolOffPeriodInMillis: how long the CoolOffPeriod the action should before reemit
     *  thresholdname: the name of threshold we are tuning  (e.g. node_duress.cpu_threshold, search_heap_threshold)
     *  dimension: indicates whether the resource unit is caused by shard/task level searchbackpressure cancellation stats
     *  Step Size in percentage: how much should the threshold change in percentage
     */
    private boolean canUpdate;
    private long coolOffPeriodInMillis;
    private String thresholdName;
    private String dimension;
    private double stepSizeInPercentage;

    public SearchBackPressureAction(
            final AppContext appContext,
            final boolean canUpdate,
            final long coolOffPeriodInMillis,
            final String thresholdName,
            final String dimension,
            final double stepSizeInPercentage) {
        super(appContext);
        this.canUpdate = canUpdate;
        this.coolOffPeriodInMillis = coolOffPeriodInMillis;
        this.thresholdName = thresholdName;
        this.dimension = dimension;
        this.stepSizeInPercentage = stepSizeInPercentage;
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

    /* TO DO: Discuss the impact of SearchBackPressureAction
     * since our action only modify the threhsold settings of Search Back Pressure Service instead of actual Resource
     * No Impact should be put as the Impact Vector for this action so other actions would not be affected by Searchbp-specific actions
     */
    @Override
    public Map<NodeKey, ImpactVector> impact() {
        Map<NodeKey, ImpactVector> impact = new HashMap<>();
        for (NodeKey key : impactedNodes()) {
            impact.put(key, NO_IMPACT);
        }
        return impact;
    }

    public String getThresholdName() {
        return thresholdName;
    }

    public String getDimension() {
        return dimension;
    }

    public double getStepSizeInPercentage() {
        return stepSizeInPercentage;
    }

    @Override
    public String summary() {
        Summary summary =
                new Summary(
                        thresholdName,
                        dimension,
                        stepSizeInPercentage,
                        DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
                        canUpdate);
        return summary.toJson();
    }

    public static final class Builder {
        public static final boolean DEFAULT_CAN_UPDATE = true;

        private final AppContext appContext;
        private final String thresholdName;
        private final String dimension;
        private boolean canUpdate;
        private double stepSizeInPercentage;
        private long coolOffPeriodInMillis;

        private Builder(
                final AppContext appContext,
                final String thresholdName,
                final String dimension,
                final long coolOffPeriodInMillis) {
            this.appContext = appContext;
            this.thresholdName = thresholdName;
            this.dimension = dimension;
            this.coolOffPeriodInMillis = coolOffPeriodInMillis;
            this.canUpdate = DEFAULT_CAN_UPDATE;
        }

        public Builder stepSizeInPercentage(double stepSizeInPercentage) {
            this.stepSizeInPercentage = stepSizeInPercentage;
            return this;
        }

        public Builder coolOffPeriodInMillis(long coolOffPeriodInMillis) {
            this.coolOffPeriodInMillis = coolOffPeriodInMillis;
            return this;
        }

        public SearchBackPressureAction build() {
            return new SearchBackPressureAction(
                    appContext,
                    canUpdate,
                    coolOffPeriodInMillis,
                    thresholdName,
                    dimension,
                    stepSizeInPercentage);
        }
    }

    /* Write Static Class Summary to conver the Searchbp Action POJO to JSON Object
     * Key fields to be included
     *  1. ThresholdName: name of the SearchBackPressure threshold to be tuned
     *  2. Dimension of the action (Shard/Task)
     *  3. StepSizeInPercentage to change the threshold
     *  4. CoolOffPeriodInMillis for the action
     *  5. canUpdate (whether the action should be emitted)
     */
    public static class Summary {
        public static final String THRESHOLD_NAME = "thresholdName";
        public static final String SEARCHBP_DIMENSION = "searchbpDimension";
        public static final String STEP_SIZE_IN_PERCENTAGE = "stepSizeInPercentage";
        public static final String COOL_OFF_PERIOD = "coolOffPeriodInMillis";
        public static final String CAN_UPDATE = "canUpdate";

        @SerializedName(value = THRESHOLD_NAME)
        private String thresholdName;

        @SerializedName(value = SEARCHBP_DIMENSION)
        private String searchbpSettingDimension;

        @SerializedName(value = STEP_SIZE_IN_PERCENTAGE)
        private double stepSizeInPercentage;

        @SerializedName(value = COOL_OFF_PERIOD)
        private long coolOffPeriodInMillis;

        @SerializedName(value = CAN_UPDATE)
        private boolean canUpdate;

        public Summary(
                String thresholdName,
                String searchbpSettingDimension,
                double stepSizeInPercentage,
                long coolOffPeriodInMillis,
                boolean canUpdate) {
            this.thresholdName = thresholdName;
            this.searchbpSettingDimension = searchbpSettingDimension;
            this.stepSizeInPercentage = stepSizeInPercentage;
            this.coolOffPeriodInMillis = coolOffPeriodInMillis;
            this.canUpdate = canUpdate;
        }

        /*
         * ThresholdName is the name of the setting to be modified
         * e.g. node_duress.cpu_threshold, node_duress.search_heap_threshold
         */
        public String getThresholdName() {
            return thresholdName;
        }

        public String getSearchbpSettingDimension() {
            return searchbpSettingDimension;
        }

        public double getStepSizeInPercentage() {
            return stepSizeInPercentage;
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
