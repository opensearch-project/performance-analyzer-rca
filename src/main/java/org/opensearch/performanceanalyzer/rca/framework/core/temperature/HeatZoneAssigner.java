/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core.temperature;

/** Given the units of the resource consumed and the total consumption and */
public class HeatZoneAssigner {
    public enum Zone {
        HOT,
        WARM,
        LUKE_WARM,
        COLD
    }

    public static Zone assign(
            final TemperatureVector.NormalizedValue consumed,
            final TemperatureVector.NormalizedValue nodeAvg,
            final TemperatureVector.NormalizedValue threshold) {
        Zone zone;
        if (consumed.isGreaterThan(nodeAvg)) {
            TemperatureVector.NormalizedValue deviation = consumed.diff(nodeAvg);
            if (deviation.isGreaterThan(threshold)) {
                zone = Zone.HOT;
            } else {
                zone = Zone.WARM;
            }
        } else {
            TemperatureVector.NormalizedValue deviation = nodeAvg.diff(consumed);
            if (deviation.isGreaterThan(threshold)) {
                zone = Zone.COLD;
            } else {
                zone = Zone.LUKE_WARM;
            }
        }
        return zone;
    }
}
