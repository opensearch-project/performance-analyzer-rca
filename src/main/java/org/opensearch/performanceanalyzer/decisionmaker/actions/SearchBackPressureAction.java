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
     *  thresholdName: the name of threshold we are tuning  (e.g. node_duress.cpu_threshold, search_heap_threshold)
     *  dimension: indicates whether the resource unit is caused by shard/task level searchbackpressure cancellation stats
     *  Step Size in percentage: how much should the threshold change in percentage
     */
    private boolean canUpdate;
    private long coolOffPeriodInMillis;
    private String thresholdName;

    private SearchbpDimension dimension;
    private SearchbpThresholdActionDirection direction;
    private double stepSizeInPercentage;

    public SearchBackPressureAction(
            final AppContext appContext,
            final boolean canUpdate,
            final long coolOffPeriodInMillis,
            final String thresholdName,
            final SearchbpDimension dimension,
            final SearchbpThresholdActionDirection direction,
            final double stepSizeInPercentage) {
        super(appContext);
        this.canUpdate = canUpdate;
        this.coolOffPeriodInMillis = coolOffPeriodInMillis;
        this.thresholdName = thresholdName;
        this.dimension = dimension;
        this.direction = direction;
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

    /* Search Back Pressure Decider/Policy only tunes searchbackpressure related thresholds (e.g. search_backpressure.search_task_heap_threshold)
     * and it does not correlate directly with any current dimension in the ImpactVector (e.g. CPU/HEAP).
     * And the current Searchbp actions only adjust heap related Searchbp Thresholds for now.
     * Dimensions in ImpactVector is used by collator to determine which action should be emitted to Publisher,
     * eventually which actions should the downstream class execute. So if there are 2 actions emitting in the same time, one increase CPU and one decrease it, the collator cancel out the actions.
     * However, since for Searchbp Actions we only tune the searchbp threshold once per time (it's impossible for 2 actions emitting in the same time that increase and decrease searchbackpressure heap usage threshold).
     * Therefore, we put no Impact for ImpactVector for Searchbp Actions.
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
        return dimension.toString();
    }

    public String getDirection() {
        return direction.toString();
    }

    public double getStepSizeInPercentage() {
        return stepSizeInPercentage;
    }

    @Override
    public String summary() {
        Summary summary =
                new Summary(
                        thresholdName,
                        dimension.toString(),
                        direction.toString(),
                        stepSizeInPercentage,
                        DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
                        canUpdate);
        return summary.toJson();
    }

    @Override
    public String toString() {
        return summary();
    }

    public static final class Builder {
        public static final boolean DEFAULT_CAN_UPDATE = true;

        private final AppContext appContext;
        private final String thresholdName;
        private final SearchbpDimension dimension;
        private final SearchbpThresholdActionDirection direction;
        private boolean canUpdate;
        private double stepSizeInPercentage;
        private long coolOffPeriodInMillis;

        private Builder(
                final AppContext appContext,
                final String thresholdName,
                final SearchbpDimension dimension,
                final SearchbpThresholdActionDirection direction,
                final long coolOffPeriodInMillis) {
            this.appContext = appContext;
            this.thresholdName = thresholdName;
            this.dimension = dimension;
            this.direction = direction;
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
                    direction,
                    stepSizeInPercentage);
        }
    }

    /* Write Static Class Summary to conver the Searchbp Action POJO to JSON Object
     * Key fields to be included
     *  1. ThresholdName: name of the SearchBackPressure threshold to be tuned
     *  2. Dimension of the action (Shard/Task)
     *  3. Direction of the action (Increase/Decrease)
     *  3. StepSizeInPercentage to change the threshold
     *  4. CoolOffPeriodInMillis for the action
     *  5. canUpdate (whether the action should be emitted)
     */
    public static class Summary {
        public static final String THRESHOLD_NAME = "thresholdName";
        public static final String SEARCHBP_DIMENSION = "searchbpDimension";
        public static final String DIRECTION = "direction";
        public static final String STEP_SIZE_IN_PERCENTAGE = "stepSizeInPercentage";
        public static final String COOL_OFF_PERIOD = "coolOffPeriodInMillis";
        public static final String CAN_UPDATE = "canUpdate";

        @SerializedName(value = THRESHOLD_NAME)
        private String thresholdName;

        @SerializedName(value = SEARCHBP_DIMENSION)
        private String searchbpSettingDimension;

        @SerializedName(value = DIRECTION)
        private String direction;

        @SerializedName(value = STEP_SIZE_IN_PERCENTAGE)
        private double stepSizeInPercentage;

        @SerializedName(value = COOL_OFF_PERIOD)
        private long coolOffPeriodInMillis;

        @SerializedName(value = CAN_UPDATE)
        private boolean canUpdate;

        public Summary(
                String thresholdName,
                String searchbpSettingDimension,
                String direction,
                double stepSizeInPercentage,
                long coolOffPeriodInMillis,
                boolean canUpdate) {
            this.thresholdName = thresholdName;
            this.searchbpSettingDimension = searchbpSettingDimension;
            this.direction = direction;
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

        public String getDirection() {
            return direction;
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

    // enum to indicate to increase/decrease the threshold
    public enum SearchbpThresholdActionDirection {
        INCREASE(SearchbpThresholdActionDirection.Constants.INCREASE),
        DECREASE(SearchbpThresholdActionDirection.Constants.DECREASE);

        private final String value;

        SearchbpThresholdActionDirection(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static class Constants {
            public static final String INCREASE = "increase";
            public static final String DECREASE = "decrease";
        }
    }

    // enum to indicate to whether the action is caused by shard/task level searchbackpressure
    // cancellation
    public enum SearchbpDimension {
        SHARD(SearchbpDimension.Constants.SHARD),
        TASK(SearchbpDimension.Constants.TASK);

        private final String value;

        SearchbpDimension(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static class Constants {
            public static final String SHARD = "shard";
            public static final String TASK = "task";
        }
    }
}
