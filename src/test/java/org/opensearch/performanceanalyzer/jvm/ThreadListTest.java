/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2020-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.performanceanalyzer.jvm;


import java.lang.management.ThreadInfo;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.OSMetricsGeneratorFactory;

// This test only runs in linux systems as the some of the static members of the ThreadList
// class are specific to Linux.
public class ThreadListTest {
    @Before
    public void before() {
        org.junit.Assume.assumeNotNull(OSMetricsGeneratorFactory.getInstance());
    }

    @Test
    public void testNullThreadInfo() {
        String propertyName = "clk.tck";
        String old_clk_tck = System.getProperty(propertyName);
        System.setProperty(propertyName, "100");
        ThreadInfo[] infos = ThreadList.getAllThreadInfos();
        // Artificially injecting a null to simulate that the thread id does not exist
        // any more and therefore the corresponding threadInfo is null.
        infos[0] = null;

        ThreadList.parseAllThreadInfos(infos);

        /*Map<String, AtomicInteger> counters = StatsCollector.instance().getCounters();

        Assert.assertEquals(
                counters.get(PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.getValues(WriterMetrics.JVM_THREAD_ID_NO_LONGER_EXISTS).toString()).get(), 1);*/

        if (old_clk_tck != null) {
            System.setProperty(propertyName, old_clk_tck);
        }
    }
}
