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
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
                    InvocationTargetException, InstantiationException {
        return (AnalysisGraph)
                Class.forName(rcaConf.getAnalysisGraphEntryPoint())
                        .getDeclaredConstructor()
                        .newInstance();
    }

    public static List<ConnectedComponent> getAnalysisGraphComponents(RcaConf rcaConf)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
                    InstantiationException, IllegalAccessException {
        AnalysisGraph graph = getAnalysisGraphImplementor(rcaConf);
        return getAnalysisGraphComponents(graph);
    }

    public static List<ConnectedComponent> getAnalysisGraphComponents(String analysisGraphClass)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
                    InstantiationException, IllegalAccessException {
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

    public static boolean doTagsMatch(Node<?> node, RcaConf conf) {
        Map<String, String> rcaTagMap = conf.getTagMap();
        for (Map.Entry<String, String> tag : node.getTags().entrySet()) {
            String rcaConfTagvalue = rcaTagMap.get(tag.getKey());
            return tag.getValue() != null
                    && Arrays.asList(tag.getValue().split(",")).contains(rcaConfTagvalue);
        }
        return true;
    }

    public static boolean shouldExecuteLocally(Node<?> node, RcaConf conf) {
        final Map<String, String> confTagMap = conf.getTagMap();
        final Map<String, String> nodeTagMap = node.getTags();

        if (confTagMap != null && nodeTagMap != null) {
            final String hostLocus = confTagMap.get(RcaConsts.RcaTagConstants.TAG_LOCUS);
            final String nodeLoci = nodeTagMap.get(RcaConsts.RcaTagConstants.TAG_LOCUS);
            if (nodeLoci != null && !nodeLoci.isEmpty()) {
                List<String> nodeLociStrings =
                        Arrays.asList(nodeLoci.split(RcaConsts.RcaTagConstants.SEPARATOR));
                return nodeLociStrings.contains(hostLocus);
            }
        }

        // By default, if no tags are specified, execute the nodes locally.
        return true;
    }
}
