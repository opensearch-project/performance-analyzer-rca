/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;

public class QueueRejectionRcaConfigTest {

    private RcaConf rcaConf;
    private RcaConf rcaOldConf;

    @Before
    public void init() {
        String rcaConfPath =
                Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca_elected_cluster_manager.conf")
                        .toString();
        String rcaOldConfPath =
                Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca_cluster_manager.conf").toString();
        rcaConf = new RcaConf(rcaConfPath);
        rcaOldConf = new RcaConf(rcaOldConfPath);
    }

    @Test
    public void testReadConfig() {
        QueueRejectionRcaConfig config = new QueueRejectionRcaConfig(rcaConf);
        Assert.assertEquals(400, config.getRejectionTimePeriodInSeconds());

        QueueRejectionRcaConfig oldConfig = new QueueRejectionRcaConfig(rcaOldConf);
        Assert.assertEquals(
                QueueRejectionRcaConfig.DEFAULT_REJECTION_TIME_PERIOD_IN_SECONDS,
                oldConfig.getRejectionTimePeriodInSeconds());
    }
}
