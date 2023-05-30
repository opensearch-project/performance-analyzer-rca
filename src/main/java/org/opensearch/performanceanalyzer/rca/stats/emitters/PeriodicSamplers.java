/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.emitters;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.SampleAggregator;

public class PeriodicSamplers implements Runnable {
    private static final Logger LOG = LogManager.getLogger(PeriodicSamplers.class);
    private final SampleAggregator aggregator;
    private final List<ISampler> allSamplers;
    private final ScheduledExecutorService executor;

    ScheduledFuture<?> future;

    public PeriodicSamplers(
            SampleAggregator aggregator, List<ISampler> samplers, long freq, TimeUnit timeUnit) {
        this.aggregator = aggregator;
        this.allSamplers = samplers;

        this.executor =
                Executors.newScheduledThreadPool(
                        1, new ThreadFactoryBuilder().setNameFormat("resource-sampler-%d").build());
        this.future = this.executor.scheduleAtFixedRate(this, 0, freq, timeUnit);
        startExceptionHandlingThread();
    }

    @Override
    public void run() {
        for (ISampler sampler : allSamplers) {
            sampler.sample(aggregator);
        }
    }

    private void startExceptionHandlingThread() {
        new Thread(
                        () -> {
                            while (true) {
                                try {
                                    future.get();
                                } catch (CancellationException cex) {
                                    LOG.info("Periodic sampler cancellation requested.");
                                } catch (Exception ex) {
                                    LOG.error("Resource state poller exception cause:", ex);
                                }
                            }
                        })
                .start();
    }
}
