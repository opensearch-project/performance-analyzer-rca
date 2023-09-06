/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;

import static org.mockito.Mockito.atLeastOnce;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

import java.util.concurrent.ArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.commons.config.ConfigStatus;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.rca.RcaTestHelper;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;
import org.opensearch.performanceanalyzer.threads.ThreadProvider;
import org.opensearch.performanceanalyzer.threads.exceptions.PAThreadException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalhostConnectionUtil.class, PerformanceAnalyzerApp.class})
@PowerMockIgnore({
    "com.sun.org.apache.xerces.*",
    "javax.xml.*",
    "org.xml.*",
    "javax.management.*",
    "org.w3c.*"
})
public class PerformanceAnalyzerAppTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Before
    public void setup() {
        System.setProperty("performanceanalyzer.metrics.log.enabled", "False");
        RcaTestHelper.cleanUpLogs();
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

    @Test
    public void testStartReaderThreadAllAttemptFail() throws Exception {
        ThreadProvider threadProvider = new ThreadProvider();
        AppContext appContext = new AppContext();

        PowerMockito.mockStatic(LocalhostConnectionUtil.class);
        ReaderMetricsProcessor readerMetricsProcessor = mock(ReaderMetricsProcessor.class);
        doThrow(new RuntimeException("Force Crashing Reader Thread"))
                .when(readerMetricsProcessor)
                .run();
        PowerMockito.whenNew(ReaderMetricsProcessor.class)
                .withAnyArguments()
                .thenReturn(readerMetricsProcessor);

        PowerMockito.spy(PerformanceAnalyzerApp.class);
        doNothing().when(PerformanceAnalyzerApp.class, "cleanupAndExit");

        // PA Disable Success
        doNothing().when(LocalhostConnectionUtil.class, "disablePA");
        PerformanceAnalyzerApp.startReaderThread(appContext, threadProvider);
        Assert.assertTrue(
                "READER_RESTART_PROCESSING metric missing",
                RcaTestHelper.verifyStatException(
                        StatExceptionCode.READER_RESTART_PROCESSING.toString()));
        Assert.assertTrue(
                "READER_ERROR_PA_DISABLE_SUCCESS metric missing",
                RcaTestHelper.verifyStatException(
                        StatExceptionCode.READER_ERROR_PA_DISABLE_SUCCESS.toString()));
        verifyPrivate(PerformanceAnalyzerApp.class, atLeastOnce()).invoke("cleanupAndExit");

        // PA Disable Fail
        doThrow(new RuntimeException("Failed to disable PA"))
                .when(LocalhostConnectionUtil.class, "disablePA");
        PerformanceAnalyzerApp.startReaderThread(appContext, threadProvider);
        Assert.assertTrue(
                "READER_RESTART_PROCESSING metric missing",
                RcaTestHelper.verifyStatException(
                        StatExceptionCode.READER_RESTART_PROCESSING.toString()));
        Assert.assertTrue(
                "READER_ERROR_PA_DISABLE_FAILED metric missing",
                RcaTestHelper.verifyStatException(
                        StatExceptionCode.READER_ERROR_PA_DISABLE_FAILED.toString()));
        verifyPrivate(PerformanceAnalyzerApp.class, atLeastOnce()).invoke("cleanupAndExit");
    }
}
