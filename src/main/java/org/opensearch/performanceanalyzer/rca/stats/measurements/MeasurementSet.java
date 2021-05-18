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

package org.opensearch.performanceanalyzer.rca.stats.measurements;


import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;

/** This is a marker interface to bring all measurement sets under one type. */
public interface MeasurementSet {
    /**
     * The statistics that should be calculated for this measurement
     *
     * @return The list of statistics to be calculated for this measurement.
     */
    List<Statistics> getStatsList();

    /**
     * The name of the measurement.
     *
     * @return The name of the measurement.
     */
    String getName();

    /**
     * The unit of measurement. This is not used for calculation but just for reference.
     *
     * @return The string representation of the unit.
     */
    String getUnit();
}
