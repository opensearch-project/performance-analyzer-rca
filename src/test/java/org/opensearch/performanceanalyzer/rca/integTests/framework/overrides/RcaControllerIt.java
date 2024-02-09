/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.overrides;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.ClientServers;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.net.GRPCConnectionManager;
import org.opensearch.performanceanalyzer.rca.RcaController;
import org.opensearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import org.opensearch.performanceanalyzer.rca.framework.core.ConnectedComponent;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaUtil;
import org.opensearch.performanceanalyzer.rca.scheduler.RCAScheduler;
import org.opensearch.performanceanalyzer.rca.scheduler.RcaSchedulerState;
import org.opensearch.performanceanalyzer.threads.ThreadProvider;
import org.opensearch.performanceanalyzer.util.WaitFor;

public class RcaControllerIt extends RcaController {
    private final String rcaPath;
    private List<ConnectedComponent> rcaGraphComponents;
    private RcaItMetricsDBProvider rcaItMetricsDBProvider;

    public RcaControllerIt(
            ThreadProvider threadProvider,
            ScheduledExecutorService netOpsExecutorService,
            GRPCConnectionManager grpcConnectionManager,
            ClientServers clientServers,
            String rca_enabled_conf_location,
            long rcaStateCheckIntervalMillis,
            long nodeRoleCheckPeriodicityMillis,
            AllMetrics.NodeRole nodeRole,
            final AppContext appContext,
            final Queryable dbProvider) {
        super(
                threadProvider,
                netOpsExecutorService,
                grpcConnectionManager,
                clientServers,
                rca_enabled_conf_location,
                rcaStateCheckIntervalMillis,
                nodeRoleCheckPeriodicityMillis,
                appContext,
                dbProvider);
        this.currentRole = nodeRole;
        this.rcaPath = rca_enabled_conf_location;
    }

    @Override
    protected List<ConnectedComponent> getRcaGraphComponents(RcaConf rcaConf)
            throws ClassNotFoundException,
                    NoSuchMethodException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException {
        if (rcaGraphComponents != null) {
            return rcaGraphComponents;
        } else {
            return super.getRcaGraphComponents(rcaConf);
        }
    }

    @Override
    protected RcaConf getRcaConfForMyRole(AllMetrics.NodeRole nodeRole) {
        RcaConfIt rcaConfIt = new RcaConfIt(super.getRcaConfForMyRole(nodeRole));
        rcaConfIt.setRcaDataStorePath(rcaPath);
        return rcaConfIt;
    }

    public void setDbProvider(final RcaItMetricsDBProvider db) throws InterruptedException {
        dbProvider = db;
        rcaItMetricsDBProvider = db;
        RCAScheduler sched = getRcaScheduler();

        // The change is optional and only happens in the next line if the scheduler is already
        // running.
        // If the scheduler is not running at the moment, then it will pick up the new DB when it
        // starts
        // next.
        if (sched != null) {
            sched.setQueryable(db);
        }
    }

    public RcaItMetricsDBProvider getDbProvider() {
        return rcaItMetricsDBProvider;
    }

    public void setRcaGraphComponents(Class rcaGraphClass)
            throws NoSuchMethodException,
                    IllegalAccessException,
                    InvocationTargetException,
                    InstantiationException {
        AnalysisGraph graphObject =
                (AnalysisGraph) rcaGraphClass.getDeclaredConstructor().newInstance();
        this.rcaGraphComponents = RcaUtil.getAnalysisGraphComponents(graphObject);
    }

    public void waitForRcaState(RcaSchedulerState state) throws Exception {
        WaitFor.waitFor(
                () -> getRcaScheduler() != null && getRcaScheduler().getState() == state,
                20,
                TimeUnit.SECONDS);
    }
}
