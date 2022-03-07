/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails.Id;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails.Ip;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class HeapSizeIncreaseAction extends SuppressibleAction {

    public static final String NAME = "HeapSizeIncreaseAction";
    private static final String ID_KEY = "Id";
    private static final String IP_KEY = "Ip";
    private final NodeKey node;
    private static final long DEFAULT_COOL_OFF_PERIOD_IN_MILLIS = TimeUnit.DAYS.toMillis(3);

    public HeapSizeIncreaseAction(@Nonnull final AppContext appContext) {
        super(appContext);
        this.node = new NodeKey(appContext.getMyInstanceDetails());
    }

    /** Constructor used when building the action from a summary. */
    public HeapSizeIncreaseAction(final NodeKey nodeKey, final AppContext appContext) {
        super(appContext);
        this.node = nodeKey;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public long coolOffPeriodInMillis() {
        return DEFAULT_COOL_OFF_PERIOD_IN_MILLIS;
    }

    @Override
    public List<NodeKey> impactedNodes() {

        return appContext.getDataNodeInstances().stream()
                .map(NodeKey::new)
                .collect(Collectors.toList());
    }

    @Override
    public Map<NodeKey, ImpactVector> impact() {
        final Map<NodeKey, ImpactVector> impactMap = new HashMap<>();
        for (NodeKey nodeKey : impactedNodes()) {
            final ImpactVector impactVector = new ImpactVector();
            impactVector.decreasesPressure(ImpactVector.Dimension.HEAP);
            impactMap.put(nodeKey, impactVector);
        }

        return impactMap;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String summary() {
        JsonObject summaryJson = new JsonObject();
        summaryJson.addProperty(ID_KEY, node.getNodeId().toString());
        summaryJson.addProperty(IP_KEY, node.getHostAddress().toString());

        return summaryJson.toString();
    }

    public static HeapSizeIncreaseAction fromSummary(
            @Nonnull final String summary, @Nonnull final AppContext appContext) {
        JsonObject jsonObject = JsonParser.parseString(summary).getAsJsonObject();
        NodeKey node =
                new NodeKey(
                        new Id(jsonObject.get(ID_KEY).getAsString()),
                        new Ip(jsonObject.get(IP_KEY).getAsString()));

        return new HeapSizeIncreaseAction(node, appContext);
    }
}
