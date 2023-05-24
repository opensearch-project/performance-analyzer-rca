/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.config.PluginSettings;
import org.opensearch.performanceanalyzer.commons.event_process.Event;
import org.opensearch.performanceanalyzer.commons.event_process.EventProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.config.overrides.ConfigOverridesApplier;
import org.opensearch.performanceanalyzer.rca.RcaControllerHelper;
import org.opensearch.performanceanalyzer.util.JsonConverter;

public class ClusterDetailsEventProcessor implements EventProcessor {
    private static final Logger LOG = LogManager.getLogger(ClusterDetailsEventProcessor.class);
    /** keep a volatile immutable list to make the read/write to this list thread safe. */
    private volatile ImmutableList<NodeDetails> nodesDetails = null;

    private final ConfigOverridesApplier overridesApplier;

    public ClusterDetailsEventProcessor() {
        this(new ConfigOverridesApplier());
    }

    public ClusterDetailsEventProcessor(final ConfigOverridesApplier overridesApplier) {
        this.overridesApplier = overridesApplier;
    }

    public ClusterDetailsEventProcessor(final ClusterDetailsEventProcessor other) {
        if (other.nodesDetails != null) {
            ImmutableList.Builder builder = new ImmutableList.Builder<NodeDetails>();
            for (final NodeDetails oldDetails : other.nodesDetails) {
                builder.add(new NodeDetails(oldDetails));
            }
            this.nodesDetails = builder.build();
            this.overridesApplier = other.getOverridesApplier();
        } else {
            this.overridesApplier = new ConfigOverridesApplier();
        }
    }

    @Override
    public void initializeProcessing(long startTime, long endTime) {}

    @Override
    public void finalizeProcessing() {}

    @Override
    public void processEvent(Event event) {
        String[] lines = event.value.split(System.lineSeparator());
        if (lines.length < 4) {
            // We expect at-least 4 lines as the first line is always timestamp,
            // the second line is the list of overridden rca conf values,
            // the third line is the timestamp of when the last override was set,
            // and there must be at least one OpenSearch node in a cluster.
            LOG.error(
                    "ClusterDetails contain less items than expected. " + "Expected 4, found: {}",
                    event.value);
            return;
        }

        // An example node_metrics data is something like this for a two node cluster:
        // {"current_time":1566414001749}
        // {"overrides": {"enabled": {}, "disabled": {}}}
        // 1566414001749
        // {"ID":"4sqG_APMQuaQwEW17_6zwg","HOST_ADDRESS":"10.212.73.121"}
        // {"ID":"OVH94mKXT5ibeqvDoAyTeg","HOST_ADDRESS":"10.212.78.83"}
        //
        // The line 0 is timestamp that can be skipped. So we allocated size of
        // the array is one less than the list.

        String overridesJson = lines[1];
        String overrideUpdatedTimestamp = lines[2];

        overridesApplier.applyOverride(overridesJson, overrideUpdatedTimestamp);

        final List<NodeDetails> tmpNodesDetails = new ArrayList<>();

        // Just to keep track of duplicate node ids.
        Set<String> ids = new HashSet<>();

        for (int i = 3; i < lines.length; ++i) {
            NodeDetails nodeDetails = new NodeDetails(lines[i]);

            // Include nodeIds we haven't seen so far.
            if (ids.add(nodeDetails.getId())) {
                tmpNodesDetails.add(nodeDetails);
            } else {
                LOG.info("node id {}, logged twice.", nodeDetails.getId());
            }
        }
        setNodesDetails(tmpNodesDetails);
    }

    @Override
    public boolean shouldProcessEvent(Event event) {
        return event.key.contains(PerformanceAnalyzerMetrics.sNodesPath);
    }

    @Override
    public void commitBatchIfRequired() {}

    public void setNodesDetails(final List<NodeDetails> nodesDetails) {
        this.nodesDetails = ImmutableList.copyOf(nodesDetails);
    }

    public List<NodeDetails> getNodesDetails() {
        if (nodesDetails != null) {
            return nodesDetails.asList();
        } else {
            return Collections.emptyList();
        }
    }

    public ConfigOverridesApplier getOverridesApplier() {
        return this.overridesApplier;
    }

    public List<NodeDetails> getDataNodesDetails() {
        List<NodeDetails> allNodes = getNodesDetails();
        if (allNodes.size() > 0) {
            return allNodes.stream()
                    .filter(p -> p.getRole().equals(AllMetrics.NodeRole.DATA.toString()))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public NodeDetails getCurrentNodeDetails() {
        List<NodeDetails> allNodes = getNodesDetails();
        if (allNodes.size() > 0) {
            return allNodes.get(0);
        } else {
            return null;
        }
    }

    public static class NodeDetails {

        private String id;
        private String hostAddress;
        private String role;
        private Boolean isClusterManager;
        private int grpcPort = PluginSettings.instance().getRpcPort();

        NodeDetails(String stringifiedMetrics) {
            Map<String, Object> map = JsonConverter.createMapFrom(stringifiedMetrics);
            id = (String) map.get(AllMetrics.NodeDetailColumns.ID.toString());
            hostAddress = (String) map.get(AllMetrics.NodeDetailColumns.HOST_ADDRESS.toString());
            role = (String) map.get(AllMetrics.NodeDetailColumns.ROLE.toString());
            Object isClusterManagerNodeObject =
                    map.get(AllMetrics.NodeDetailColumns.IS_CLUSTER_MANAGER_NODE.toString());
            isClusterManager =
                    isClusterManagerNodeObject != null
                            ? (Boolean) isClusterManagerNodeObject
                            : null;
        }

        public NodeDetails(
                AllMetrics.NodeRole role, String id, String hostAddress, boolean isClusterManager) {
            this(role, id, hostAddress, isClusterManager, PluginSettings.instance().getRpcPort());
        }

        public NodeDetails(final NodeDetails other) {
            if (other != null) {
                this.id = other.id;
                this.hostAddress = other.hostAddress;
                this.isClusterManager = other.isClusterManager;
                this.role = other.role;
            }
        }

        public NodeDetails(
                AllMetrics.NodeRole role,
                String id,
                String hostAddress,
                boolean isClusterManager,
                int grpcPort) {
            this.role = role.toString();
            this.id = id;
            this.hostAddress = hostAddress;
            this.isClusterManager = isClusterManager;
            this.grpcPort = grpcPort;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder
                    .append("{")
                    .append("id:")
                    .append(id)
                    .append(" hostAddress:")
                    .append(hostAddress)
                    .append(" role:")
                    .append(role)
                    .append(" isClusterManager:")
                    .append(isClusterManager)
                    .append("}");
            return stringBuilder.toString();
        }

        public String getId() {
            return id;
        }

        @Nullable
        public String getHostAddress() {
            return hostAddress;
        }

        @Nullable
        public String getRole() {
            return role;
        }

        public boolean getIsClusterManagerNode() {
            // TODO : this is added to support backward compatibility for _cat/cluster_manager fix.
            //  We can remove this later if the writer changes has been adapted by all clusters in
            // fleet.
            // query cat cluster_manager api directly to read the node's role in case the
            // ClusterDetailsEvent
            // received from writer does not have "IS_CLUSTER_MANAGER_NODE" field.
            if (isClusterManager == null) {
                final String electedClusterManagerHostAddress =
                        RcaControllerHelper.getElectedClusterManagerHostAddress();
                isClusterManager =
                        this.hostAddress.equalsIgnoreCase(electedClusterManagerHostAddress);
            }
            return isClusterManager;
        }

        public int getGrpcPort() {
            return grpcPort;
        }
    }
}
