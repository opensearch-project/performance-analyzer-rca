/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.measurements;


import java.util.Arrays;
import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;

public enum MeasurementSetTestHelper implements MeasurementSet {
    TEST_MEASUREMENT1(
            "TestMeasurement1",
            "micros",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.MIN)),
    TEST_MEASUREMENT2("TestMeasurement2", "micros", Arrays.asList(Statistics.COUNT)),
    TEST_MEASUREMENT3("TestMeasurement3", "micros", Arrays.asList(Statistics.COUNT)),
    TEST_MEASUREMENT4("TestMeasurement4", "micros", Arrays.asList(Statistics.SAMPLE)),
    TEST_MEASUREMENT5("TestMeasurement5", "micros", Arrays.asList(Statistics.SUM)),
    TEST_MEASUREMENT6("TestMeasurement6", "micros", Arrays.asList(Statistics.NAMED_COUNTERS)),
    JVM_FREE_MEM_SAMPLER("jvmFreeMemorySampler", "bytes", Arrays.asList(Statistics.SAMPLE));

    private String name;
    private String unit;
    private List<Statistics> statsList;

    MeasurementSetTestHelper(String name, String unit, List<Statistics> statisticList) {
        this.name = name;
        this.unit = unit;
        this.statsList = statisticList;
    }

    public String toString() {
        return new StringBuilder(name).append("-").append(unit).toString();
    }

    @Override
    public List<Statistics> getStatsList() {
        return statsList;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUnit() {
        return unit;
    }
}
