/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class TimedFlipFlopDetectorTest {
    private TimedFlipFlopDetector flipFlopDetector;
    private static ImpactVector increaseAll = new ImpactVector();
    private static ImpactVector decreaseAll = new ImpactVector();
    private static ImpactVector noImpact = new ImpactVector();

    @BeforeClass
    public static void setupClass() {
        increaseAll.increasesPressure(ImpactVector.Dimension.values());
        decreaseAll.decreasesPressure(ImpactVector.Dimension.values());
        noImpact.noImpact(ImpactVector.Dimension.values());
    }

    @Before
    public void setup() {
        flipFlopDetector = new TimedFlipFlopDetector(2, TimeUnit.SECONDS);
    }

    /**
     * Tests that a Impact.DECREASES_PRESSURE followed by a Impact.INCREASES_PRESSURE is the only
     * order of impacts that should be considered a flip flop
     */
    @Test
    public void testIsFlipFlopImpact() {
        int flipFlopImpacts = 0;
        for (ImpactVector.Impact a : ImpactVector.Impact.values()) {
            for (ImpactVector.Impact b : ImpactVector.Impact.values()) {
                if (flipFlopDetector.isFlipFlopImpact(a, b)) {
                    flipFlopImpacts++;
                }
            }
        }
        Assert.assertEquals(1, flipFlopImpacts);
        Assert.assertTrue(
                flipFlopDetector.isFlipFlopImpact(
                        ImpactVector.Impact.DECREASES_PRESSURE,
                        ImpactVector.Impact.INCREASES_PRESSURE));
    }

    /** Tests that we can identify flip flops for any two impact vectors (u,v) */
    @Test
    public void testIsClash() {
        // Stability changes are not flip flops
        Assert.assertFalse(flipFlopDetector.isFlipFlopVector(increaseAll, decreaseAll));
        // Multiple increases are not flip flops, cool off can handle throttling these
        Assert.assertFalse(flipFlopDetector.isFlipFlopVector(increaseAll, increaseAll));
        // Multiple decreases are not flip flops, cool off can handle throttling these
        Assert.assertFalse(flipFlopDetector.isFlipFlopVector(decreaseAll, decreaseAll));
        // noImpact dimensions shouldn't contribute to flip flop detection
        Assert.assertFalse(flipFlopDetector.isFlipFlopVector(decreaseAll, noImpact));
        Assert.assertFalse(flipFlopDetector.isFlipFlopVector(increaseAll, noImpact));
        Assert.assertFalse(flipFlopDetector.isFlipFlopVector(noImpact, increaseAll));
        Assert.assertFalse(flipFlopDetector.isFlipFlopVector(noImpact, decreaseAll));
        // An increase after a decrease that hasn't expired is the definition of a flip flop
        Assert.assertTrue(flipFlopDetector.isFlipFlopVector(decreaseAll, increaseAll));
    }

    private static Action mockAction(NodeKey key, ImpactVector impactVector) {
        Action action = Mockito.mock(Action.class);
        Map<NodeKey, ImpactVector> impactMap = new HashMap<>();
        impactMap.put(key, impactVector);
        when(action.impact()).thenReturn(impactMap);
        return action;
    }

    @Test
    public void testIsFlipFlop() throws Exception {
        // Setup mock actions, action followed by flipFlopAction is a flip flop
        NodeKey nodeKey =
                new NodeKey(new InstanceDetails.Id("A"), new InstanceDetails.Ip("127.0.0.1"));
        Action action = mockAction(nodeKey, decreaseAll);
        Action flipflopAction = mockAction(nodeKey, increaseAll);
        // Update the flipFlopDetector so that the last "executed" action is action
        flipFlopDetector.recordAction(action);
        // Verify that the basic flip flop test succeeds
        Assert.assertTrue(flipFlopDetector.isFlipFlop(flipflopAction));
        // Verify that once the expiry period has passed, flipFlopAction is no longer considered a
        // flip flop
        Thread.sleep(2500L);
        Assert.assertFalse(flipFlopDetector.isFlipFlop(flipflopAction));
    }

    /**
     * This test verifies that multiple actions applied to a node are all considered when
     * determining a flip flop.
     *
     * <p>e.g. c clashes with b; apply a; apply b; verify that c is considered a flip flop
     */
    @Test
    public void testMultipleActionFlipFlop() throws Exception {
        // Setup test objects, flip flops are (b, c) and (a, d)
        NodeKey nodeKey =
                new NodeKey(new InstanceDetails.Id("A"), new InstanceDetails.Ip("127.0.0.1"));
        ImpactVector aVector = new ImpactVector();
        aVector.decreasesPressure(ImpactVector.Dimension.HEAP);
        Action a = mockAction(nodeKey, aVector);
        ImpactVector bVector = new ImpactVector();
        bVector.decreasesPressure(ImpactVector.Dimension.CPU);
        Action b = mockAction(nodeKey, bVector);
        ImpactVector cVector = new ImpactVector();
        cVector.increasesPressure(ImpactVector.Dimension.CPU);
        Action c = mockAction(nodeKey, cVector);
        ImpactVector dVector = new ImpactVector();
        dVector.increasesPressure(ImpactVector.Dimension.HEAP);
        Action d = mockAction(nodeKey, dVector);
        // Apply a, verify b and c are not flip flops, verify d is a flip flop
        flipFlopDetector.recordAction(a);
        Assert.assertFalse(flipFlopDetector.isFlipFlop(b));
        Assert.assertFalse(flipFlopDetector.isFlipFlop(c));
        Assert.assertTrue(flipFlopDetector.isFlipFlop(d));
        Thread.sleep(1000L);
        // Apply b, verify that c is now a flip flop
        flipFlopDetector.recordAction(b);
        Assert.assertTrue(flipFlopDetector.isFlipFlop(c));
        // Let a expire, verify d is no longer a flip flop
        Thread.sleep(1500L);
        Assert.assertFalse(flipFlopDetector.isFlipFlop(d));
        // Let b expire, verify c is no longer a flip flop
        Thread.sleep(1000L);
        Assert.assertFalse(flipFlopDetector.isFlipFlop(c));
    }

    /**
     * If the same action (based on its ImpactVector) is applied multiple times, its expiry should
     * reset
     */
    @Test
    public void testFlipFlopRefresh() throws Exception {
        NodeKey nodeKey =
                new NodeKey(new InstanceDetails.Id("A"), new InstanceDetails.Ip("127.0.0.1"));
        Action action = mockAction(nodeKey, decreaseAll);
        Action flipflopAction = mockAction(nodeKey, increaseAll);
        flipFlopDetector.recordAction(action);
        Thread.sleep(1000L);
        // refresh the action
        flipFlopDetector.recordAction(action);
        Thread.sleep(1500L);
        // verify that even though the 2s expiry for the initial action has passed, the action is
        // still
        // around because it was refreshed
        Assert.assertTrue(flipFlopDetector.isFlipFlop(flipflopAction));
    }
}
