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

package org.opensearch.performanceanalyzer.decisionmaker.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCache;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class ModifyQueueCapacityActionTest {

    private AppContext testAppContext;
    private NodeConfigCache dummyCache;
    private RcaConf rcaConf;

    public ModifyQueueCapacityActionTest() {
        testAppContext = new AppContext();
        dummyCache = testAppContext.getNodeConfigCache();
        String rcaConfPath = Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString();
        rcaConf = new RcaConf(rcaConfPath);
    }

    @Test
    public void testIncreaseCapacity() {
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        dummyCache.put(node1, ResourceUtil.WRITE_QUEUE_CAPACITY, 500);
        ModifyQueueCapacityAction.Builder builder =
                ModifyQueueCapacityAction.newBuilder(
                        node1, ResourceEnum.WRITE_THREADPOOL, testAppContext, rcaConf);
        ModifyQueueCapacityAction modifyQueueCapacityAction = builder.increase(true).build();
        Assert.assertNotNull(modifyQueueCapacityAction);
        assertTrue(
                modifyQueueCapacityAction.getDesiredCapacity()
                        > modifyQueueCapacityAction.getCurrentCapacity());
        assertTrue(modifyQueueCapacityAction.isActionable());
        Assert.assertEquals(
                ModifyQueueCapacityAction.Builder.DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
                modifyQueueCapacityAction.coolOffPeriodInMillis());
        Assert.assertEquals(
                ResourceEnum.WRITE_THREADPOOL, modifyQueueCapacityAction.getThreadPool());
        assertEquals(1, modifyQueueCapacityAction.impactedNodes().size());

        Map<ImpactVector.Dimension, ImpactVector.Impact> impact =
                modifyQueueCapacityAction.impact().get(node1).getImpact();
        Assert.assertEquals(
                ImpactVector.Impact.INCREASES_PRESSURE, impact.get(ImpactVector.Dimension.HEAP));
        Assert.assertEquals(
                ImpactVector.Impact.INCREASES_PRESSURE, impact.get(ImpactVector.Dimension.CPU));
        Assert.assertEquals(
                ImpactVector.Impact.INCREASES_PRESSURE, impact.get(ImpactVector.Dimension.NETWORK));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.RAM));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.DISK));
    }

    @Test
    public void testDecreaseCapacity() {
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        dummyCache.put(node1, ResourceUtil.SEARCH_QUEUE_CAPACITY, 1500);
        ModifyQueueCapacityAction.Builder builder =
                ModifyQueueCapacityAction.newBuilder(
                        node1, ResourceEnum.SEARCH_THREADPOOL, testAppContext, rcaConf);
        ModifyQueueCapacityAction modifyQueueCapacityAction = builder.increase(false).build();
        Assert.assertNotNull(modifyQueueCapacityAction);
        assertTrue(
                modifyQueueCapacityAction.getDesiredCapacity()
                        < modifyQueueCapacityAction.getCurrentCapacity());
        assertTrue(modifyQueueCapacityAction.isActionable());
        assertEquals(
                ModifyQueueCapacityAction.Builder.DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
                modifyQueueCapacityAction.coolOffPeriodInMillis());
        Assert.assertEquals(
                ResourceEnum.SEARCH_THREADPOOL, modifyQueueCapacityAction.getThreadPool());
        assertEquals(1, modifyQueueCapacityAction.impactedNodes().size());

        Map<ImpactVector.Dimension, ImpactVector.Impact> impact =
                modifyQueueCapacityAction.impact().get(node1).getImpact();
        Assert.assertEquals(
                ImpactVector.Impact.DECREASES_PRESSURE, impact.get(ImpactVector.Dimension.HEAP));
        Assert.assertEquals(
                ImpactVector.Impact.DECREASES_PRESSURE, impact.get(ImpactVector.Dimension.CPU));
        Assert.assertEquals(
                ImpactVector.Impact.DECREASES_PRESSURE, impact.get(ImpactVector.Dimension.NETWORK));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.RAM));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.DISK));
    }

    @Test
    public void testBounds() throws Exception {
        final String configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"queue-settings\": { "
                        + "\"search\": { "
                        + "\"upper-bound\": 500, "
                        + "\"lower-bound\": 100 "
                        + "}, "
                        + "\"write\": { "
                        + "\"upper-bound\": 50, "
                        + "\"lower-bound\": 10 "
                        + "} "
                        + "} "
                        + "} "
                        + "}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);

        // Test Upper Bound
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        dummyCache.put(node1, ResourceUtil.SEARCH_QUEUE_CAPACITY, 500);
        ModifyQueueCapacityAction searchQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.SEARCH_THREADPOOL, testAppContext, conf)
                        .increase(true)
                        .build();
        assertEquals(500, searchQueueAction.getDesiredCapacity());
        assertFalse(searchQueueAction.isActionable());

        dummyCache.put(node1, ResourceUtil.WRITE_QUEUE_CAPACITY, 50);
        ModifyQueueCapacityAction writeQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.WRITE_THREADPOOL, testAppContext, conf)
                        .increase(true)
                        .build();
        assertEquals(50, writeQueueAction.getDesiredCapacity());
        assertFalse(writeQueueAction.isActionable());

        // Test Lower Bound
        node1 = new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        dummyCache.put(node1, ResourceUtil.SEARCH_QUEUE_CAPACITY, 100);
        searchQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.SEARCH_THREADPOOL, testAppContext, conf)
                        .increase(false)
                        .build();
        assertEquals(100, searchQueueAction.getDesiredCapacity());
        assertFalse(searchQueueAction.isActionable());

        dummyCache.put(node1, ResourceUtil.WRITE_QUEUE_CAPACITY, 10);
        writeQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.WRITE_THREADPOOL, testAppContext, conf)
                        .increase(false)
                        .build();
        assertEquals(10, writeQueueAction.getDesiredCapacity());
        assertFalse(writeQueueAction.isActionable());
    }

    @Test
    public void testMinMaxOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"queue-settings\": { "
                        + "\"search\": { "
                        + "\"upper-bound\": 500, "
                        + "\"lower-bound\": 100 "
                        + "}, "
                        + "\"write\": { "
                        + "\"upper-bound\": 50, "
                        + "\"lower-bound\": 10 "
                        + "} "
                        + "} "
                        + "} "
                        + "}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);

        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        dummyCache.put(node1, ResourceUtil.SEARCH_QUEUE_CAPACITY, 200);
        dummyCache.put(node1, ResourceUtil.WRITE_QUEUE_CAPACITY, 20);

        // Test Max Override
        ModifyQueueCapacityAction searchQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.SEARCH_THREADPOOL, testAppContext, conf)
                        .setDesiredCapacityToMax()
                        .build();
        assertEquals(500, searchQueueAction.getDesiredCapacity());
        assertTrue(searchQueueAction.isActionable());

        ModifyQueueCapacityAction writeQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.WRITE_THREADPOOL, testAppContext, conf)
                        .setDesiredCapacityToMax()
                        .build();
        assertEquals(50, writeQueueAction.getDesiredCapacity());
        assertTrue(writeQueueAction.isActionable());

        // Test Min Override
        searchQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.SEARCH_THREADPOOL, testAppContext, conf)
                        .setDesiredCapacityToMin()
                        .build();
        assertEquals(100, searchQueueAction.getDesiredCapacity());
        assertTrue(searchQueueAction.isActionable());

        writeQueueAction =
                ModifyQueueCapacityAction.newBuilder(
                                node1, ResourceEnum.WRITE_THREADPOOL, testAppContext, conf)
                        .setDesiredCapacityToMin()
                        .build();
        assertEquals(10, writeQueueAction.getDesiredCapacity());
        assertTrue(writeQueueAction.isActionable());
    }

    @Test
    public void testMutedAction() {
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        dummyCache.put(node1, ResourceUtil.SEARCH_QUEUE_CAPACITY, 2000);
        ModifyQueueCapacityAction.Builder builder =
                ModifyQueueCapacityAction.newBuilder(
                        node1, ResourceEnum.SEARCH_THREADPOOL, testAppContext, rcaConf);
        ModifyQueueCapacityAction modifyQueueCapacityAction = builder.increase(true).build();

        testAppContext.updateMutedActions(ImmutableSet.of(modifyQueueCapacityAction.name()));

        assertFalse(modifyQueueCapacityAction.isActionable());
    }

    @Test
    public void testSummary() {
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        dummyCache.put(node1, ResourceUtil.WRITE_QUEUE_CAPACITY, 500);
        ModifyQueueCapacityAction.Builder builder =
                ModifyQueueCapacityAction.newBuilder(
                        node1, ResourceEnum.WRITE_THREADPOOL, testAppContext, rcaConf);
        ModifyQueueCapacityAction modifyQueueCapacityAction = builder.increase(true).build();
        String summary = modifyQueueCapacityAction.summary();

        ModifyQueueCapacityAction objectFromSummary =
                ModifyQueueCapacityAction.fromSummary(summary, testAppContext);
        assertEquals(
                modifyQueueCapacityAction.getCurrentCapacity(),
                objectFromSummary.getCurrentCapacity());
        assertEquals(
                modifyQueueCapacityAction.getDesiredCapacity(),
                objectFromSummary.getDesiredCapacity());
        Assert.assertEquals(
                modifyQueueCapacityAction.getThreadPool(), objectFromSummary.getThreadPool());
    }

    private void assertNoImpact(NodeKey node, ModifyQueueCapacityAction modifyQueueCapacityAction) {
        Map<ImpactVector.Dimension, ImpactVector.Impact> impact =
                modifyQueueCapacityAction.impact().get(node).getImpact();
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.HEAP));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.CPU));
        Assert.assertEquals(
                ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.NETWORK));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.RAM));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.DISK));
    }
}
