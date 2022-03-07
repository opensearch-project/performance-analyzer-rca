/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
