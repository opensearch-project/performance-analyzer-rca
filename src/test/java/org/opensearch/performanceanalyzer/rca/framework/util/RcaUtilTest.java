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

package org.opensearch.performanceanalyzer.rca.framework.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.core.Node;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

@Category(GradleTaskForRca.class)
public class RcaUtilTest {

    @Test
    public void doTagsMatch() {
        Node<MetricFlowUnit> node = new CPU_Utilization(5);
        node.addTag("locus", "data-node");
        RcaConf rcaConf = new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());
        assertTrue(RcaUtil.doTagsMatch(node, rcaConf));
    }

    @Test
    public void noMatchWithExtraNodeTags() {
        Node<MetricFlowUnit> node = new CPU_Utilization(5);
        node.addTag("locus", "data-node");
        // This is the extra tag.
        node.addTag("name", "sifi");
        RcaConf rcaConf = new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());
        assertFalse(RcaUtil.doTagsMatch(node, rcaConf));
    }

    @Test
    public void noNodeTagsIsAMatch() {
        Node<MetricFlowUnit> node = new CPU_Utilization(5);
        RcaConf rcaConf = new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());
        assertTrue(RcaUtil.doTagsMatch(node, rcaConf));
    }

    @Test
    public void existingTagWithDifferentValueNoMatch() {
        Node<MetricFlowUnit> node = new CPU_Utilization(5);
        node.addTag("locus", "master-node");
        RcaConf rcaConf = new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());
        assertFalse(RcaUtil.doTagsMatch(node, rcaConf));
    }
}
