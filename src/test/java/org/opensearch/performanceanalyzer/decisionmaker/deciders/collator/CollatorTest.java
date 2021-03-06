/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.collator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ImpactVector;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.Decider;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.Decision;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class CollatorTest {

    private Collator testCollator;

    @Mock private Decider mockDecider1;

    @Mock private Decider mockDecider2;

    @Mock private Comparator<Action> mockComparator;

    @Mock private Decision decision1;

    @Mock private Decision decision2;

    @Mock private Action moveShardAction1;

    @Mock private Action moveShardAction2;

    @Mock private Action moveShardAction3;

    private final ImpactAssessor impactAssessor = new ImpactAssessor();

    private final NodeKey nodeA =
            new NodeKey(new InstanceDetails.Id("node A"), new InstanceDetails.Ip("1.2.3.4"));
    private final NodeKey nodeB =
            new NodeKey(new InstanceDetails.Id("node B"), new InstanceDetails.Ip("5.6.7.8"));
    private final NodeKey nodeC =
            new NodeKey(new InstanceDetails.Id("node C"), new InstanceDetails.Ip("9.10.11.12"));

    private String moveShardActionName = "MoveShard";

    private Map<NodeKey, ImpactVector> moveShardImpact1 =
            ImmutableMap.of(
                    nodeA, buildShardMoveOutImpactVector(),
                    nodeB, buildShardMoveInImpactVector());

    private Map<NodeKey, ImpactVector> moveShardImpact2 =
            ImmutableMap.of(
                    nodeB, buildShardMoveOutImpactVector(),
                    nodeC, buildShardMoveInImpactVector());

    private Map<NodeKey, ImpactVector> moveShardImpact3 =
            ImmutableMap.of(
                    nodeC, buildShardMoveOutImpactVector(),
                    nodeA, buildShardMoveInImpactVector());

    @Before
    public void setup() {
        initMocks(this);
        this.testCollator =
                new Collator(impactAssessor, mockComparator, mockDecider1, mockDecider2);
        setupActions();
        setupDecisions();
    }

    @Test
    public void testCollatorAcyclicImpactDecisions() {
        when(mockDecider1.getFlowUnits()).thenReturn(Collections.singletonList(decision1));
        when(mockDecider2.getFlowUnits()).thenReturn(Collections.singletonList(decision2));
        // fix some order for the test.
        when(mockComparator.compare(eq(moveShardAction1), eq(moveShardAction2))).thenReturn(-1);

        Decision decision = testCollator.operate();

        assertEquals(1, decision.getActions().size());
        Assert.assertEquals(moveShardAction2, decision.getActions().get(0));
    }

    @Test
    public void testCollatorCyclicImpactDecisions() {
        when(decision1.getActions()).thenReturn(Arrays.asList(moveShardAction1, moveShardAction3));
        when(mockDecider1.getFlowUnits()).thenReturn(Collections.singletonList(decision1));
        when(mockDecider2.getFlowUnits()).thenReturn(Collections.singletonList(decision2));
        when(mockComparator.compare(any(Action.class), any(Action.class))).thenReturn(0);
        this.testCollator =
                new Collator(
                        impactAssessor,
                        new Collator.ImpactBasedActionComparator(),
                        mockDecider1,
                        mockDecider2);

        Decision decision = testCollator.operate();

        assertEquals(1, decision.getActions().size());
        Assert.assertEquals(moveShardAction3, decision.getActions().get(0));
    }

    @Test
    public void testCollatorEmptyActions() {
        when(decision1.getActions()).thenReturn(Collections.emptyList());
        when(decision2.getActions()).thenReturn(Collections.emptyList());
        when(mockDecider1.getFlowUnits()).thenReturn(Collections.singletonList(decision1));
        when(mockDecider2.getFlowUnits()).thenReturn(Collections.singletonList(decision2));
        this.testCollator =
                new Collator(
                        impactAssessor,
                        new Collator.ImpactBasedActionComparator(),
                        mockDecider1,
                        mockDecider2);

        Decision decision = testCollator.operate();

        assertTrue(decision.getActions().isEmpty());
    }

    @Test
    public void testNoDeciders() {
        testCollator = new Collator(impactAssessor, mockComparator);

        final Decision decision = testCollator.operate();

        assertTrue(decision.getActions().isEmpty());
    }

    public void setupDecisions() {
        when(decision1.getActions()).thenReturn(Collections.singletonList(moveShardAction1));
        when(decision2.getActions()).thenReturn(Collections.singletonList(moveShardAction2));
    }

    public void setupActions() {
        when(moveShardAction1.name()).thenReturn(moveShardActionName);
        when(moveShardAction1.impact()).thenReturn(moveShardImpact1);
        when(moveShardAction1.impactedNodes())
                .thenReturn(new ArrayList<>(moveShardImpact1.keySet()));

        when(moveShardAction2.name()).thenReturn(moveShardActionName);
        when(moveShardAction2.impact()).thenReturn(moveShardImpact2);
        when(moveShardAction2.impactedNodes())
                .thenReturn(new ArrayList<>(moveShardImpact2.keySet()));

        when(moveShardAction3.name()).thenReturn(moveShardActionName);
        when(moveShardAction3.impact()).thenReturn(moveShardImpact3);
        when(moveShardAction3.impactedNodes())
                .thenReturn(new ArrayList<>(moveShardImpact3.keySet()));
    }

    public ImpactVector buildShardMoveOutImpactVector() {
        final ImpactVector impactVector = new ImpactVector();
        impactVector.decreasesPressure(ImpactVector.Dimension.CPU);
        impactVector.decreasesPressure(ImpactVector.Dimension.HEAP);

        return impactVector;
    }

    public ImpactVector buildShardMoveInImpactVector() {
        final ImpactVector impactVector = new ImpactVector();
        impactVector.increasesPressure(ImpactVector.Dimension.CPU);
        impactVector.increasesPressure(ImpactVector.Dimension.HEAP);

        return impactVector;
    }
}
