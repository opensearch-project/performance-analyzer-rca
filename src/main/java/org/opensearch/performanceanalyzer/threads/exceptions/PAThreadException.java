/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.threads.exceptions;

import org.opensearch.performanceanalyzer.PerformanceAnalyzerThreads;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;

/** Exception that is thrown when one of the PA threads run into an unhandled exception. */
public class PAThreadException extends Exception {

    private final PerformanceAnalyzerThreads paThread;

    private Throwable innerThrowable;

    public PAThreadException(final PerformanceAnalyzerThreads paThread, final Throwable throwable) {
        this.paThread = paThread;
        this.innerThrowable = throwable;
    }

    /**
     * Gets the name of the thread that threw an unhandled exception.
     *
     * @return The name of the thread.
     */
    public String getPaThreadName() {
        return paThread.toString();
    }

    /**
     * Gets the counter against which we need to record an error metric.
     *
     * @return The {@link StatExceptionCode} value that represents the error metric name.
     */
    public StatExceptionCode getExceptionCode() {
        return paThread.getThreadExceptionCode();
    }

    /**
     * Gets the actual {@link Throwable} thrown by the thread.
     *
     * @return The throwable.
     */
    public Throwable getInnerThrowable() {
        return innerThrowable;
    }
}
