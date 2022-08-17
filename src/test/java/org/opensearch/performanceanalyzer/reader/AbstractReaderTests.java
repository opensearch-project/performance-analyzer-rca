/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.Ignore;
import org.opensearch.performanceanalyzer.AbstractTests;
import org.opensearch.performanceanalyzer.collectors.DiskMetrics;
import org.opensearch.performanceanalyzer.collectors.HeapMetricsCollector;
import org.opensearch.performanceanalyzer.collectors.MetricStatus;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metrics.MetricDimension;
import org.opensearch.performanceanalyzer.metrics.PerformanceAnalyzerMetrics;

// import
// org.opensearch.performanceanalyzer.collectors.ClusterManagerServiceMetrics.ClusterManagerPendingStatus;
// import
// org.opensearch.performanceanalyzer.collectors.NodeDetailsCollector.NodeDetailsStatus;
// import
// org.opensearch.performanceanalyzer.collectors.NodeStatsMetricsCollector;
// import
// org.opensearch.performanceanalyzer.reader.MetricPropertiesTests.FailureCondition;
// import
// org.opensearch.performanceanalyzer.collectors.ClusterManagerServiceMetrics.ClusterManagerPendingStatus;
// import
// org.opensearch.performanceanalyzer.collectors.NodeDetailsCollector.NodeDetailsStatus;
// import
// org.opensearch.performanceanalyzer.collectors.NodeStatsMetricsCollector;

@Ignore
public class AbstractReaderTests extends AbstractTests {
    protected final String DB_URL;

    protected final Connection conn;

    public AbstractReaderTests() throws SQLException, ClassNotFoundException {
        // make sure the sqlite classes and driver are loaded
        Class.forName("org.sqlite.JDBC");
        DB_URL = "jdbc:sqlite:";
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
    }

    protected Condition getDimensionEqCondition(
            MetricDimension dimentionHeader, String dimensionName) {
        return DSL.field(dimentionHeader.toString(), String.class).eq(dimensionName);
    }

    protected String createRelativePath(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            sb.append(path);
            sb.append(File.separator);
        }
        return sb.toString();
    }

    protected void write(File f, boolean append, String... input) throws IOException {
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, append)))) {
            for (String line : input) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    protected String getCurrentMilliSeconds(long currentTimeMillis) {
        return new StringBuilder()
                .append("{\"")
                .append(PerformanceAnalyzerMetrics.METRIC_CURRENT_TIME)
                .append("\"")
                .append(PerformanceAnalyzerMetrics.sKeyValueDelimitor)
                .append(currentTimeMillis)
                .append("}")
                .toString();
    }

    protected String createDiskMetrics(
            String name, double utilization, double await, double serviceRate) {
        StringBuffer value = new StringBuffer();

        value.append(new DiskMetrics(name, utilization, await, serviceRate).serialize());

        return value.toString();
    }

    protected String createPendingTaskMetrics(int pendingTaskCount) {
        StringBuffer value = new StringBuffer();

        value.append(new ClusterManagerPendingStatus(pendingTaskCount).serialize());

        return value.toString();
    }

    protected String createHeapMetrics(
            AllMetrics.GCType name, long committed, long init, long max, long used) {
        return new HeapMetricsCollector.HeapStatus(name.toString(), committed, init, max, used)
                .serialize();
    }

    protected String createHeapMetrics(
            AllMetrics.GCType name, long collectionCount, long collectionTime) {
        return new HeapMetricsCollector.HeapStatus(name.toString(), collectionCount, collectionTime)
                .serialize();
    }

    protected static String createNodeDetailsMetrics(
            String id, String ipAddress, boolean isClusterManagerNode) {
        return createNodeDetailsMetrics(
                id, ipAddress, AllMetrics.NodeRole.DATA, isClusterManagerNode);
    }

    protected static String createNodeDetailsMetrics(
            String id,
            String ipAddress,
            AllMetrics.NodeRole nodeRole,
            boolean isClusterManagerNode) {
        StringBuffer value = new StringBuffer();
        value.append(
                new NodeDetailsStatus(id, ipAddress, nodeRole, isClusterManagerNode).serialize());
        return value.toString();
    }

    protected static ClusterDetailsEventProcessor.NodeDetails createNodeDetails(
            String id, String ipAddress, boolean isClusterManagerNode) {
        return new ClusterDetailsEventProcessor.NodeDetails(
                createNodeDetailsMetrics(id, ipAddress, isClusterManagerNode));
    }

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    public static class ClusterManagerPendingStatus extends MetricStatus {
        private final int pendingTasksCount;

        public ClusterManagerPendingStatus(int pendingTasksCount) {
            this.pendingTasksCount = pendingTasksCount;
        }

        @JsonProperty(AllMetrics.ClusterManagerPendingValue.Constants.PENDING_TASKS_COUNT_VALUE)
        public int getPendingTasksCount() {
            return pendingTasksCount;
        }
    }

    public static class NodeDetailsStatus extends MetricStatus {
        private String id;
        private String hostAddress;
        private String nodeRole;
        private boolean isClusterManagerNode;

        public NodeDetailsStatus(
                String id,
                String hostAddress,
                AllMetrics.NodeRole nodeRole,
                boolean isClusterManagerNode) {
            super();
            this.id = id;
            this.hostAddress = hostAddress;
            this.nodeRole = nodeRole.role();
            this.isClusterManagerNode = isClusterManagerNode;
        }

        @JsonProperty(AllMetrics.NodeDetailColumns.Constants.ID_VALUE)
        public String getID() {
            return id;
        }

        @JsonProperty(AllMetrics.NodeDetailColumns.Constants.HOST_ADDRESS_VALUE)
        public String getHostAddress() {
            return hostAddress;
        }

        @JsonProperty(AllMetrics.NodeDetailColumns.Constants.ROLE_VALUE)
        public String getNodeRole() {
            return nodeRole;
        }

        @JsonProperty(AllMetrics.NodeDetailColumns.Constants.IS_CLUSTER_MANAGER_NODE)
        public boolean getIsClusterManagerNode() {
            return isClusterManagerNode;
        }
    }
}
