/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opensearch.performanceanalyzer.rca.framework.core.NestedConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.util.range.Range;

// spotless:off
/**
 * Defines config for request-size controller threshold tuning.
 * Example config below - for heap percent between 0% and 75% set threshold to 15%
 *
 * <p>Configs are expected in the following json format:
 * {
 *   "admission-control-rca": {
 *     "request-size": {
 *       "heap-range": [
 *         {
 *           "lower-bound": 0,
 *           "upper-bound": 80,
 *           "threshold": 15.0
 *         },
 *         ...
 *       ]
 *     }
 *   }
 * }
 */
// spotless:on
public class AdmissionControlRcaConfig {
    public static final String CONFIG_NAME = "admission-control-rca";
    private static final String REQUEST_SIZE = "request-size";
    private static final String GLOBAL_JVMMP = "global-jvmmp";
    private static final String UPPER_BOUND = "upper-bound";
    private static final String LOWER_BOUND = "lower-bound";
    private static final String THRESHOLD = "threshold";
    private static final String RANGE = "heap-range";
    private static final double DEFAULT_LOWER_BOUND = 0;
    private static final double DEFAULT_UPPER_BOUND = 100;
    private static final double DEFAULT_GLOBAL_JVMMP_THRESHOLD = 85;
    private static final double DEFAULT_REQUEST_SIZE_THRESHOLD = 10;

    private final NestedConfig admissionControlConfig;
    private final ControllerConfig requestSizeControllerConfig;
    private final ControllerConfig globalJVMMPControllerConfig;

    public AdmissionControlRcaConfig(final RcaConf rcaConf) {
        Map<String, Object> actionConfig = rcaConf.getRcaConfigSettings();
        admissionControlConfig = new NestedConfig(CONFIG_NAME, actionConfig);
        requestSizeControllerConfig = new ControllerConfig(REQUEST_SIZE, admissionControlConfig);
        globalJVMMPControllerConfig = new ControllerConfig(GLOBAL_JVMMP, admissionControlConfig);
    }

    public ControllerConfig getGlobalJVMMPControllerConfig() {
        return globalJVMMPControllerConfig;
    }

    public ControllerConfig getRequestSizeControllerConfig() {
        return requestSizeControllerConfig;
    }

    public static class ControllerConfig {
        private NestedConfig controllerConfig;
        private List<Range> heapRangeConfiguration = Collections.emptyList();

        public ControllerConfig(String controller, NestedConfig settingsConfig) {
            controllerConfig = new NestedConfig(controller, settingsConfig.getValue());
            if (controllerConfig.getValue() != null) {
                List<Map> rangeList = (List<Map>) controllerConfig.getValue().get(RANGE);
                if (Objects.isNull(rangeList) == false) {
                    heapRangeConfiguration =
                            rangeList.stream()
                                    .map(
                                            r ->
                                                    new Range(
                                                            Double.parseDouble(
                                                                    r.getOrDefault(
                                                                                    LOWER_BOUND,
                                                                                    DEFAULT_LOWER_BOUND)
                                                                            .toString()),
                                                            Double.parseDouble(
                                                                    r.getOrDefault(
                                                                                    UPPER_BOUND,
                                                                                    DEFAULT_UPPER_BOUND)
                                                                            .toString()),
                                                            Double.parseDouble(
                                                                    r.getOrDefault(
                                                                                    THRESHOLD,
                                                                                    DEFAULT_REQUEST_SIZE_THRESHOLD)
                                                                            .toString())))
                                    .collect(Collectors.toList());
                }
            }
        }

        public List<Range> getHeapRangeConfiguration() {
            return heapRangeConfiguration;
        }

        @Override
        public String toString() {
            return String.format(
                    "ControllerConfig{controllerConfig=%s, heapRangeConfiguration=%s}",
                    controllerConfig, heapRangeConfiguration);
        }
    }
}
