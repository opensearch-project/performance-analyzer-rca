/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.measurements;


import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;

/** This is a marker interface to bring all measurement sets under one type. */
public interface MeasurementSet {
    /**
     * The statistics that should be calculated for this measurement
     *
     * @return The list of statistics to be calculated for this measurement.
     */
    List<Statistics> getStatsList();

    /**
     * The name of the measurement.
     *
     * @return The name of the measurement.
     */
    String getName();

    /**
     * The unit of measurement. This is not used for calculation but just for reference.
     *
     * @return The string representation of the unit.
     */
    String getUnit();
}
