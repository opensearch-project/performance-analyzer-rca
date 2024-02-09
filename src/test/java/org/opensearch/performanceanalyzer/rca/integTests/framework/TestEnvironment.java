/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensearch.performanceanalyzer.rca.RcaControllerHelper;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaConf;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaGraph;

public class TestEnvironment {
    private final Cluster cluster;
    private final Env classLevelEnv;

    private Env currentEnv;

    public TestEnvironment(final Cluster cluster, final Class testClass) throws Exception {
        this.cluster = cluster;
        this.classLevelEnv = updateEnvironment(testClass);
    }

    private Env updateEnvironment(final Class testClass) throws Exception {
        boolean annotationsPresent =
                testClass.isAnnotationPresent(ARcaConf.class)
                        | testClass.isAnnotationPresent(ARcaGraph.class)
                        | testClass.isAnnotationPresent(ARcaConf.class);

        Env env = new Env();

        if (annotationsPresent) {
            if (testClass.isAnnotationPresent(ARcaConf.class)) {
                updateRcaConf((ARcaConf) testClass.getAnnotation(ARcaConf.class), env);
            }
            if (testClass.isAnnotationPresent(ARcaGraph.class)) {
                updateRcaGraph((ARcaGraph) testClass.getAnnotation(ARcaGraph.class), env);
            }

            if (testClass.isAnnotationPresent(AMetric.Metrics.class)
                    || testClass.isAnnotationPresent(AMetric.class)) {
                updateMetricsDB(
                        (AMetric[]) testClass.getAnnotationsByType(AMetric.class), env, true);
            }
        }
        if (env.rcaConfMap.isEmpty()) {
            updateWithDefaultRcaConfAnnotation(env);
        }
        return env;
    }

    public void updateEnvironment(final Method method) throws Exception {
        boolean annotationsPresent =
                method.isAnnotationPresent(ARcaConf.class)
                        | method.isAnnotationPresent(ARcaGraph.class)
                        | method.isAnnotationPresent(ARcaConf.class);

        this.currentEnv = new Env(this.classLevelEnv);

        if (annotationsPresent) {
            if (method.isAnnotationPresent(ARcaConf.class)) {
                updateRcaConf(method.getAnnotation(ARcaConf.class), currentEnv);
            }
            if (method.isAnnotationPresent(ARcaGraph.class)) {
                updateRcaGraph(method.getAnnotation(ARcaGraph.class), currentEnv);
            }

            if (method.isAnnotationPresent(AMetric.Metrics.class)) {
                updateMetricsDB(method.getAnnotationsByType(AMetric.class), currentEnv, true);
            }
        }
    }

    private void updateRcaConf(ARcaConf aRcaConf, Env env) {
        String clusterManagerRcaConf = aRcaConf.electedClusterManager();
        env.rcaConfMap.put(ARcaConf.Type.ELECTED_CLUSTER_MANAGER, clusterManagerRcaConf);

        String standByClusterManagerRcaConf = aRcaConf.standBy();
        env.rcaConfMap.put(ARcaConf.Type.STANDBY_CLUSTER_MANAGER, standByClusterManagerRcaConf);

        String dataNodesRcaConf = aRcaConf.dataNode();
        env.rcaConfMap.put(ARcaConf.Type.DATA_NODES, dataNodesRcaConf);

        RcaControllerHelper.set(
                dataNodesRcaConf, standByClusterManagerRcaConf, clusterManagerRcaConf);
    }

    private void updateRcaGraph(ARcaGraph aRcaGraph, Env env)
            throws NoSuchMethodException,
                    IllegalAccessException,
                    InvocationTargetException,
                    InstantiationException {
        Class graphClass = aRcaGraph.value();
        env.rcaGraphClass = graphClass;
        cluster.updateGraph(graphClass);
    }

    private void updateMetricsDB(AMetric[] metrics, Env env, boolean reloadDB) throws Exception {
        cluster.updateMetricsDB(metrics, reloadDB);
        env.isMetricsDBProviderSet = true;
    }

    private void updateWithDefaultRcaConfAnnotation(Env env) throws NoSuchMethodException {
        String clusterManagerRcaConf =
                (String) ARcaConf.class.getMethod("electedClusterManager").getDefaultValue();
        env.rcaConfMap.put(ARcaConf.Type.ELECTED_CLUSTER_MANAGER, clusterManagerRcaConf);

        String standByClusterManagerRcaConf =
                (String) ARcaConf.class.getMethod("standBy").getDefaultValue();
        env.rcaConfMap.put(ARcaConf.Type.STANDBY_CLUSTER_MANAGER, standByClusterManagerRcaConf);

        String dataNodesRcaConf = (String) ARcaConf.class.getMethod("dataNode").getDefaultValue();
        env.rcaConfMap.put(ARcaConf.Type.DATA_NODES, dataNodesRcaConf);

        RcaControllerHelper.set(
                dataNodesRcaConf, standByClusterManagerRcaConf, clusterManagerRcaConf);
    }

    public void clearUpMethodLevelEnvOverride() {
        this.currentEnv = this.classLevelEnv;
    }

    public void verifyEnvironmentSetup() throws IllegalStateException {
        List<String> annotationsNotSet = new ArrayList<>();

        if (currentEnv.rcaConfMap.isEmpty()) {
            annotationsNotSet.add(ARcaGraph.class.getSimpleName());
        }
        if (currentEnv.rcaGraphClass == null) {
            annotationsNotSet.add(ARcaGraph.class.getSimpleName());
        }
        if (!currentEnv.isMetricsDBProviderSet) {
            annotationsNotSet.add(AMetric.class.getSimpleName());
        }

        if (!annotationsNotSet.isEmpty()) {
            clearUpMethodLevelEnvOverride();
            throw new IllegalStateException("** Some annotations are not set" + annotationsNotSet);
        }
    }

    class Env {
        private final Map<ARcaConf.Type, String> rcaConfMap;
        private Class rcaGraphClass;
        private boolean isMetricsDBProviderSet;

        Env() {
            this.rcaConfMap = new HashMap<>();
            this.rcaGraphClass = null;
            this.isMetricsDBProviderSet = false;
        }

        Env(Env other) {
            this.rcaConfMap = other.rcaConfMap;
            this.rcaGraphClass = other.rcaGraphClass;
            this.isMetricsDBProviderSet = other.isMetricsDBProviderSet;
        }
    }
}
