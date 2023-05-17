/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.jvm;


import java.lang.management.ThreadInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.OSMetricsGeneratorFactory;
import org.opensearch.performanceanalyzer.rca.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;

// This test only runs in linux systems as the some of the static members of the ThreadList
// class are specific to Linux.
public class ThreadListTest {
    @Before
    public void before() {
        org.junit.Assume.assumeNotNull(OSMetricsGeneratorFactory.getInstance());
    }

    @Test
    public void testNullThreadInfo() throws InterruptedException {
        String propertyName = "clk.tck";
        String old_clk_tck = System.getProperty(propertyName);
        System.setProperty(propertyName, "100");
        ThreadInfo[] infos = ThreadList.getAllThreadInfos();
        // Artificially injecting a null to simulate that the thread id does not exist
        // any more and therefore the corresponding threadInfo is null.
        infos[0] = null;

        ThreadList.parseAllThreadInfos(infos);
        Assert.assertTrue(RcaTestHelper.verify(ExceptionsAndErrors.JVM_THREAD_ID_NO_LONGER_EXISTS));
        if (old_clk_tck != null) {
            System.setProperty(propertyName, old_clk_tck);
        }
    }
}
