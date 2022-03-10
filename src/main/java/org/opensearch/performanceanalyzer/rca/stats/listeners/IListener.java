/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.listeners;


import java.util.Set;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

/**
 * This interface is implemented by the interested parties who want to to subscribe to the
 * occurrence of a metric emission. The Aggregator makes sure it calls the listener.
 */
public interface IListener {
    Set<MeasurementSet> getMeasurementsListenedTo();

    void onOccurrence(MeasurementSet measurementSet, Number value, String key);
}
