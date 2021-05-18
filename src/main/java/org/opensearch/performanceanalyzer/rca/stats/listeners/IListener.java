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
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.stats.listeners;


import java.util.Set;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

/**
 * This interface is implemented by the interested parties who want to to subscribe to the
 * occurrence of a metric emission. The Aggregator makes sure it calls the listener.
 */
public interface IListener {
    Set<MeasurementSet> getMeasurementsListenedTo();

    void onOccurrence(MeasurementSet measurementSet, Number value, String key);
}
