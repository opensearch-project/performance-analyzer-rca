/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.collector;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCache;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

@Category(GradleTaskForRca.class)
public class NodeConfigCacheTest {

    private NodeConfigCache nodeConfigCache;
    private NodeKey nodeKey1;
    private NodeKey nodeKey2;

    @Before
    public void init() {
        this.nodeConfigCache = new NodeConfigCache();
        this.nodeKey1 =
                new NodeKey(new InstanceDetails.Id("node1"), new InstanceDetails.Ip("127.0.0.1"));
        this.nodeKey2 =
                new NodeKey(new InstanceDetails.Id("node2"), new InstanceDetails.Ip("127.0.0.2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistentKey() {
        double val = nodeConfigCache.get(nodeKey1, ResourceUtil.WRITE_QUEUE_CAPACITY);
        Assert.fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadWrongKey() {
        nodeConfigCache.put(nodeKey1, ResourceUtil.WRITE_QUEUE_CAPACITY, 2.0);
        double val = nodeConfigCache.get(nodeKey1, ResourceUtil.WRITE_QUEUE_REJECTION);
        Assert.fail();
    }

    @Test
    public void testSetAndGetValue() {
        nodeConfigCache.put(nodeKey1, ResourceUtil.WRITE_QUEUE_CAPACITY, 3.0);
        double val = nodeConfigCache.get(nodeKey1, ResourceUtil.WRITE_QUEUE_CAPACITY);
        Assert.assertEquals(3.0, val, 0.01);

        nodeConfigCache.put(nodeKey1, ResourceUtil.WRITE_QUEUE_CAPACITY, 4.0);
        val = nodeConfigCache.get(nodeKey1, ResourceUtil.WRITE_QUEUE_CAPACITY);
        Assert.assertEquals(4.0, val, 0.01);
    }
}
