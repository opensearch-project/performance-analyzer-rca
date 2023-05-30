/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;


import java.util.concurrent.ArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.commons.config.ConfigStatus;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.rca.RcaTestHelper;
import org.opensearch.performanceanalyzer.threads.ThreadProvider;
import org.opensearch.performanceanalyzer.threads.exceptions.PAThreadException;

public class PerformanceAnalyzerAppTest {

    @Before
    public void setup() {
        System.setProperty("performanceanalyzer.metrics.log.enabled", "False");
    }

    @Test
    public void testMain() {
        PerformanceAnalyzerApp.main(new String[0]);
        Assert.assertFalse(ConfigStatus.INSTANCE.haveValidConfig());
    }

    @Test
    public void testInvalidConfigStatusMain() throws InterruptedException {
        ConfigStatus.INSTANCE.setConfigurationInvalid();
        PerformanceAnalyzerApp.main(new String[0]);
        Assert.assertTrue(
                RcaTestHelper.verifyStatException(
                        StatExceptionCode.INVALID_CONFIG_RCA_AGENT_STOPPED.toString()));
    }

    @Test
    public void testStartErrorHandlingThread() throws InterruptedException {
        ThreadProvider threadProvider = new ThreadProvider();
        ArrayBlockingQueue<PAThreadException> exceptionQueue = new ArrayBlockingQueue<>(1);
        final Thread errorHandlingThread =
                PerformanceAnalyzerApp.startErrorHandlingThread(threadProvider, exceptionQueue);
        errorHandlingThread.interrupt();
        Assert.assertTrue(
                RcaTestHelper.verifyStatException(
                        StatExceptionCode.ERROR_HANDLER_THREAD_STOPPED.toString()));
    }
}
