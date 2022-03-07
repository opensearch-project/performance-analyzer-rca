/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core.temperature;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;

/**
 * This class contains the vector details which contains the raw metrics values. We have added this
 * vector to compare against the calculated normalized values of the metrics so as to catch any
 * anomalies whatsoever.
 */
public class RawMetricsVector {
    public static final String DIMENSION_KEY = "metrics_dimension";
    public static final String VALUE_KEY = "metrics_value";

    private double[] metricsValues;

    public RawMetricsVector() {
        metricsValues = new double[TemperatureDimension.values().length];
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (TemperatureDimension dim : TemperatureDimension.values()) {
            double val = metricsValues[dim.ordinal()];
            if (val != 0) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty(DIMENSION_KEY, dim.NAME);
                jsonObject.addProperty(VALUE_KEY, val);
                array.add(jsonObject);
            }
        }
        return array;
    }

    /**
     * This can be used to get raw metrics along a dimension.
     *
     * @param dimension one of the dimensions
     * @return The raw metrics value along that dimension.
     */
    @Nullable
    public double getMetricsFor(TemperatureDimension dimension) {
        return metricsValues[dimension.ordinal()];
    }

    public void updateRawMetricsForDimension(TemperatureDimension dimension, double metricsValue) {
        metricsValues[dimension.ordinal()] = metricsValue;
    }
}
