/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;


import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ActionListener;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ImpactVector;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.collator.Collator;
import org.opensearch.performanceanalyzer.plugins.Plugin;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.persistence.Persistable;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class PublisherTest {

    private static final int EVAL_INTERVAL_S = 5;
    private static Publisher publisher;

    // Mock objects
    @Mock private Collator collator;

    @Mock private Decision decision;

    @Mock private Action action;

    @Mock private ActionListener actionListener;

    @Mock private FlowUnitOperationArgWrapper flowUnitOperationArgWrapper;

    @Mock private Persistable persistable;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        publisher = new Publisher(EVAL_INTERVAL_S, collator);
        publisher.addActionListener(actionListener);
        List<Decision> decisionList = Lists.newArrayList(decision);
        Mockito.when(collator.getFlowUnits()).thenReturn(decisionList);
        Mockito.when(decision.getActions()).thenReturn(Lists.newArrayList(action));
        Mockito.when(flowUnitOperationArgWrapper.getPersistable()).thenReturn(persistable);
    }

    @Test
    public void testRejectsFlipFlops() throws Exception {
        // Setup testing objects
        NodeKey nodeKey =
                new NodeKey(new InstanceDetails.Id("A"), new InstanceDetails.Ip("127.0.0.1"));
        ImpactVector allDecrease = new ImpactVector();
        allDecrease.decreasesPressure(ImpactVector.Dimension.values());
        Map<NodeKey, ImpactVector> impactVectorMap = new HashMap<>();
        impactVectorMap.put(nodeKey, allDecrease);
        Mockito.when(action.name()).thenReturn("testIsCooledOffAction");
        Mockito.when(action.coolOffPeriodInMillis()).thenReturn(500L);
        Mockito.when(action.impact()).thenReturn(impactVectorMap);
        // Record a flip flopping action
        publisher.getFlipFlopDetector().recordAction(action);
        ImpactVector allIncrease = new ImpactVector();
        allIncrease.increasesPressure(ImpactVector.Dimension.values());
        Map<NodeKey, ImpactVector> increaseMap = new HashMap<>();
        increaseMap.put(nodeKey, allIncrease);
        Mockito.when(action.impact()).thenReturn(increaseMap);
        Thread.sleep(1000L);
        // Even though our action has cooled off, it will flip flop, so the publisher shouldn't
        // execute it
        publisher.compute(flowUnitOperationArgWrapper);
        Mockito.verify(actionListener, Mockito.times(0)).actionPublished(action);
    }

    @Test
    public void testListenerInvocations() {
        Mockito.when(collator.getFlowUnits()).thenReturn(Collections.singletonList(decision));
        Mockito.when(decision.getActions()).thenReturn(Lists.newArrayList(action));
        Mockito.when(action.name()).thenReturn("testAction");
        Mockito.when(action.coolOffPeriodInMillis()).thenReturn(0L);

        ActionListener actionListener2 = Mockito.mock(ActionListener.class);
        ActionListener testActionListener = Mockito.mock(TestActionListener.class);
        publisher.addActionListener(actionListener2);
        publisher.addActionListener(testActionListener);

        publisher.compute(flowUnitOperationArgWrapper);
        Mockito.verify(actionListener, Mockito.times(1)).actionPublished(action);
        Mockito.verify(actionListener2, Mockito.times(1)).actionPublished(action);
        Mockito.verify(testActionListener, Mockito.times(1)).actionPublished(action);
    }

    public static class TestActionListener extends Plugin implements ActionListener {

        @Override
        public void actionPublished(Action action) {
            assert true;
        }

        @Override
        public String name() {
            return "Test_Plugin";
        }
    }
}
