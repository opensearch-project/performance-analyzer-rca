/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.validator;


import org.junit.Assert;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyQueueCapacityAction;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.RcaControllerHelper;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;

public class QueueDeciderValidator implements IValidator {
    AppContext appContext;
    RcaConf rcaConf;
    long startTime;

    public QueueDeciderValidator() {
        appContext = new AppContext();
        startTime = System.currentTimeMillis();
        rcaConf = RcaControllerHelper.pickRcaConfForRole(AllMetrics.NodeRole.ELECTED_MASTER);
    }

    /**
     * {"actionName":"ModifyQueueCapacity", "resourceValue":4, "timestamp":"1599257910923",
     * "nodeId":"node1", "nodeIp":127.0.0.1, "actionable":1, "coolOffPeriod": 300000, "muted": 0
     * "summary": "Id":"DATA_0","Ip":"127.0.0.1","resource":4,"desiredCapacity":547,
     * "currentCapacity":500,"coolOffPeriodInMillis":10000,"canUpdate":true}
     */
    @Override
    public boolean checkDbObj(Object object) {
        if (object == null) {
            return false;
        }
        PersistedAction persistedAction = (PersistedAction) object;
        return checkPersistedAction(persistedAction);
    }

    /**
     * {"actionName":"ModifyQueueCapacity", "resourceValue":4, "timestamp":"1599257910923",
     * "nodeId":"node1", "nodeIp":127.0.0.1, "actionable":1, "coolOffPeriod": 300000, "muted": 0
     * "summary": "Id":"DATA_0","Ip":"127.0.0.1","resource":4,"desiredCapacity":547,
     * "currentCapacity":500,"coolOffPeriodInMillis":10000,"canUpdate":true}
     */
    private boolean checkPersistedAction(final PersistedAction persistedAction) {
        ModifyQueueCapacityAction modifyQueueCapacityAction =
                ModifyQueueCapacityAction.fromSummary(persistedAction.getSummary(), appContext);
        Assert.assertEquals(ModifyQueueCapacityAction.NAME, persistedAction.getActionName());
        Assert.assertEquals("{DATA_0}", persistedAction.getNodeIds());
        Assert.assertEquals("{127.0.0.1}", persistedAction.getNodeIps());
        Assert.assertEquals(
                ModifyQueueCapacityAction.Builder.DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
                persistedAction.getCoolOffPeriod());
        Assert.assertTrue(persistedAction.isActionable());
        Assert.assertFalse(persistedAction.isMuted());
        Assert.assertEquals(
                ResourceEnum.WRITE_THREADPOOL, modifyQueueCapacityAction.getThreadPool());
        int writeQueueStepSize =
                rcaConf.getQueueActionConfig().getStepSize(ResourceEnum.WRITE_THREADPOOL);
        Assert.assertEquals(
                500 + writeQueueStepSize, modifyQueueCapacityAction.getDesiredCapacity());
        Assert.assertEquals(500, modifyQueueCapacityAction.getCurrentCapacity());
        return true;
    }
}
