/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.listeners;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.rca.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSetTestHelper;

public class IListenerTest {
    class Listener implements IListener {
        private AtomicInteger count;

        public Listener() {
            count = new AtomicInteger(0);
        }

        @Override
        public Set<MeasurementSet> getMeasurementsListenedTo() {
            Set<MeasurementSet> set =
                    new HashSet() {
                        {
                            this.add(MeasurementSetTestHelper.TEST_MEASUREMENT1);
                            this.add(MeasurementSetTestHelper.TEST_MEASUREMENT2);
                        }
                    };
            return set;
        }

        @Override
        public void onOccurrence(MeasurementSet measurementSet, Number value, String key) {
            count.getAndIncrement();
        }
    }

    @Test
    public void onOccurrence() {
        Listener listener = new Listener();
        SampleAggregator sampleAggregator =
                new SampleAggregator(
                        listener.getMeasurementsListenedTo(),
                        listener,
                        MeasurementSetTestHelper.values());

        sampleAggregator.updateStat(MeasurementSetTestHelper.TEST_MEASUREMENT4, "", 1);
        sampleAggregator.updateStat(MeasurementSetTestHelper.TEST_MEASUREMENT1, "", 1);
        sampleAggregator.updateStat(MeasurementSetTestHelper.TEST_MEASUREMENT2, "", 1);

        Assert.assertEquals(2, listener.count.get());
    }
}
