/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;

import java.util.Iterator;
import java.util.Objects;

public class SlidingWindowTestUtil {
    public static boolean equals(
            SlidingWindow<SlidingWindowData> a, SlidingWindow<SlidingWindowData> b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        if (a.size() != b.size()) {
            return false;
        }
        Iterator<SlidingWindowData> aIt = a.windowDeque.descendingIterator();
        Iterator<SlidingWindowData> bIt = b.windowDeque.descendingIterator();
        while (aIt.hasNext()) {
            SlidingWindowData aData = aIt.next();
            SlidingWindowData bData = bIt.next();
            if (aData.getValue() != bData.getValue()
                    || aData.getTimeStamp() != bData.getTimeStamp()) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals_MUTATE(
            SlidingWindow<SlidingWindowData> a, SlidingWindow<SlidingWindowData> b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        if (a.size() != b.size()) {
            return false;
        }
        while (a.size() > 0) {
            if (a.windowDeque.getLast().getValue() != b.windowDeque.getLast().getValue()) {
                return false;
            }
            a.windowDeque.removeLast();
            b.windowDeque.removeLast();
        }
        return true;
    }
}
