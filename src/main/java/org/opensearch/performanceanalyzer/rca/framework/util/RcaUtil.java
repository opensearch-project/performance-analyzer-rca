/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import org.opensearch.performanceanalyzer.rca.framework.core.ConnectedComponent;
import org.opensearch.performanceanalyzer.rca.framework.core.Node;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.core.Stats;

public class RcaUtil {

    private static final Logger LOG = LogManager.getLogger(RcaUtil.class);

    private static AnalysisGraph getAnalysisGraphImplementor(RcaConf rcaConf)
            throws ClassNotFoundException,
                    NoSuchMethodException,
                    IllegalAccessException,
                    InvocationTargetException,
                    InstantiationException {
        return (AnalysisGraph)
                Class.forName(rcaConf.getAnalysisGraphEntryPoint())
                        .getDeclaredConstructor()
                        .newInstance();
    }

    public static List<ConnectedComponent> getAnalysisGraphComponents(RcaConf rcaConf)
            throws ClassNotFoundException,
                    NoSuchMethodException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException {
        AnalysisGraph graph = getAnalysisGraphImplementor(rcaConf);
        return getAnalysisGraphComponents(graph);
    }

    public static List<ConnectedComponent> getAnalysisGraphComponents(String analysisGraphClass)
            throws ClassNotFoundException,
                    NoSuchMethodException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException {
        AnalysisGraph graph =
                (AnalysisGraph)
                        Class.forName(analysisGraphClass).getDeclaredConstructor().newInstance();
        graph.construct();
        graph.validateAndProcess();
        return Stats.getInstance().getConnectedComponents();
    }

    public static List<ConnectedComponent> getAnalysisGraphComponents(AnalysisGraph graph) {
        graph.construct();
        graph.validateAndProcess();
        return Stats.getInstance().getConnectedComponents();
    }

    /**
     * As there is possibility for host locus tags to be hybrid, in terms of rca subscription we
     * still have to identify the host with single tag, the most priority one.
     */
    public static String getPriorityLocus(String hostLocus) {
        if (hostLocus == null || hostLocus.isEmpty()) {
            return "";
        }
        List<String> hostLociStrings =
                Arrays.asList(hostLocus.split(RcaConsts.RcaTagConstants.SEPARATOR));
        // Non-empty string was split -> guaranteed to be of size at least one.
        return hostLociStrings.get(0);
    }

    public static boolean containsAny(List<String> containerList, List<String> containedList) {
        for (String elem : containedList) {
            if (containerList.contains(elem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doTagsMatch(Node<?> node, RcaConf conf) {
        Map<String, String> rcaTagMap = conf.getTagMap();
        for (Map.Entry<String, String> tag : node.getTags().entrySet()) {
            String rcaConfTag = rcaTagMap.get(tag.getKey());
            if (rcaConfTag == null) {
                return false;
            }
            List<String> rcaConfTagStrings = Arrays.asList(rcaConfTag.split(","));

            return tag.getValue() != null
                    && containsAny(rcaConfTagStrings, Arrays.asList(tag.getValue().split(",")));
        }
        return true;
    }

    public static boolean shouldExecuteLocally(Node<?> node, RcaConf conf) {
        final Map<String, String> confTagMap = conf.getTagMap();
        final Map<String, String> nodeTagMap = node.getTags();

        if (confTagMap != null && nodeTagMap != null) {
            final String hostLoci = confTagMap.get(RcaConsts.RcaTagConstants.TAG_LOCUS);
            final String nodeLoci = nodeTagMap.get(RcaConsts.RcaTagConstants.TAG_LOCUS);
            if (nodeLoci != null && !nodeLoci.isEmpty()) {
                List<String> nodeLociStrings =
                        Arrays.asList(nodeLoci.split(RcaConsts.RcaTagConstants.SEPARATOR));
                List<String> hostLociStrings =
                        Arrays.asList(hostLoci.split(RcaConsts.RcaTagConstants.SEPARATOR));
                return containsAny(hostLociStrings, nodeLociStrings);
            }
        }

        // By default, if no tags are specified, execute the nodes locally.
        return true;
    }
}
