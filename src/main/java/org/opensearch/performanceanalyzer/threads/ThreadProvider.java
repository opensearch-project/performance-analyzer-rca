/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.threads;

import static org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode.NUM_PA_THREADS_ENDED;
import static org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode.NUM_PA_THREADS_STARTED;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerThreads;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.threads.exceptions.PAThreadException;

/** Class that wraps a given runnable in a thread with exception handling capabilities. */
public class ThreadProvider {

    private static final Logger LOG = LogManager.getLogger(ThreadProvider.class);

    /**
     * Creates a thread which executes the given runnable when started. If the given runnable throws
     * an uncaught exception, it is then written to the exception queue which will be processed by
     * the exception handler thread.
     *
     * @param innerRunnable The runnable to execute when the thread starts.
     * @param paThread The thread enum value from {@link PerformanceAnalyzerThreads}
     * @return The thread with the wrapped runnable.
     */
    public Thread createThreadForRunnable(
            final Runnable innerRunnable,
            final PerformanceAnalyzerThreads paThread,
            String threadNameAppender) {
        StringBuilder threadName = new StringBuilder(paThread.toString());
        if (!threadNameAppender.isEmpty()) {
            threadName.append("-").append(threadNameAppender);
        }
        String threadNameStr = threadName.toString();
        StatExceptionCode metric = paThread.getThreadExceptionCode();
        Thread t =
                new Thread(
                        () -> {
                            try {
                                innerRunnable.run();
                            } catch (Throwable innerThrowable) {
                                LOG.error("A thread crashed: ", innerThrowable);
                                try {
                                    PerformanceAnalyzerApp.exceptionQueue.put(
                                            new PAThreadException(paThread, innerThrowable));
                                } catch (InterruptedException e) {
                                    LOG.error(
                                            "Thread was interrupted while waiting to put an exception into the queue. "
                                                    + "Message: {}",
                                            e.getMessage(),
                                            e);
                                    StatsCollector.instance().logException(metric);
                                }
                            }
                            LOG.info("Thread: {} completed.", threadNameStr);
                            StatsCollector.instance().logException(NUM_PA_THREADS_ENDED);
                        },
                        threadNameStr);

        LOG.info("Spun up a thread with name: {}", threadNameStr);
        StatsCollector.instance().logException(NUM_PA_THREADS_STARTED);
        return t;
    }

    public Thread createThreadForRunnable(
            final Runnable innerRunnable, final PerformanceAnalyzerThreads paThread) {
        return createThreadForRunnable(innerRunnable, paThread, "");
    }
}
