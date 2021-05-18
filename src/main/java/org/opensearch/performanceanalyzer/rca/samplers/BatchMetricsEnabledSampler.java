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

package org.opensearch.performanceanalyzer.rca.samplers;


import java.util.Objects;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.rca.stats.emitters.ISampler;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;

public class BatchMetricsEnabledSampler implements ISampler {
    private final AppContext appContext;

    public BatchMetricsEnabledSampler(final AppContext appContext) {
        Objects.requireNonNull(appContext);
        this.appContext = appContext;
    }

    @Override
    public void sample(SampleAggregator sampleCollector) {
        sampleCollector.updateStat(
                ReaderMetrics.BATCH_METRICS_ENABLED, "", isBatchMetricsEnabled() ? 1 : 0);
    }

    boolean isBatchMetricsEnabled() {
        InstanceDetails currentNode = appContext.getMyInstanceDetails();
        if (currentNode != null && currentNode.getIsMaster()) {
            return ReaderMetricsProcessor.getInstance().getBatchMetricsEnabled();
        }
        return false;
    }
}
