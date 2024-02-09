/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ImpactVector {

    public enum Dimension {
        HEAP,
        CPU,
        RAM,
        DISK,
        NETWORK,
        ADMISSION_CONTROL
    }

    public enum Impact {
        NO_IMPACT,
        INCREASES_PRESSURE,
        DECREASES_PRESSURE
    }

    private Map<Dimension, Impact> impactMap = new HashMap<>();

    public ImpactVector() {
        for (Dimension d : Dimension.values()) {
            impactMap.put(d, Impact.NO_IMPACT);
        }
    }

    public Map<Dimension, Impact> getImpact() {
        return Collections.unmodifiableMap(impactMap);
    }

    public void increasesPressure(Dimension... dimensions) {
        for (Dimension dimension : dimensions) {
            impactMap.put(dimension, Impact.INCREASES_PRESSURE);
        }
    }

    public void decreasesPressure(Dimension... dimensions) {
        for (Dimension dimension : dimensions) {
            impactMap.put(dimension, Impact.DECREASES_PRESSURE);
        }
    }

    public void noImpact(Dimension... dimensions) {
        for (Dimension dimension : dimensions) {
            impactMap.put(dimension, Impact.NO_IMPACT);
        }
    }

    /**
     * Two ImpactVectors are equal if and only if they have the same impact for each of their
     * dimensions
     *
     * @param o The other ImpactVector to compare with this
     * @return true if and only if this and o have the same impact for each of their dimensions
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImpactVector that = (ImpactVector) o;
        return Objects.equals(impactMap, that.impactMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(impactMap);
    }
}
