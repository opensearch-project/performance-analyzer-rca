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

package org.opensearch.performanceanalyzer.metrics;

/**
 * This helps writing a general parser. Given a MetricValue, I can parse the metric file using the
 * values provided by the MetricValue enum. I don't need to hardcode the exact enum name in the
 * parser. The parser only needs to know this enum has a metric's values and use its members as Json
 * key to parse out the concrete metric values. See
 * src/main/java/org/opensearch/performanceanalyzer/reader/MetricProperties.java
 */
public interface MetricValue {}
