/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics;


import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsRestUtil {

    public String nodeJsonBuilder(ConcurrentHashMap<String, String> nodeResponses) {
        StringBuilder outputJson = new StringBuilder();
        outputJson.append("{");
        Set<String> nodeSet = nodeResponses.keySet();
        String[] nodes = nodeSet.toArray(new String[nodeSet.size()]);
        if (nodes.length > 0) {
            outputJson.append("\"");
            outputJson.append(nodes[0]);
            outputJson.append("\": ");
            outputJson.append(nodeResponses.get(nodes[0]));
        }

        for (int i = 1; i < nodes.length; i++) {
            outputJson.append(", \"");
            outputJson.append(nodes[i]);
            outputJson.append("\" :");
            outputJson.append(nodeResponses.get(nodes[i]));
        }

        outputJson.append("}");
        return outputJson.toString();
    }

    public List<String> parseArrayParam(Map<String, String> params, String name, boolean optional)
            throws InvalidParameterException {
        if (!optional) {
            if (!params.containsKey(name) || params.get(name).isEmpty()) {
                throw new InvalidParameterException(
                        String.format("%s parameter needs to be set", name));
            }
        }

        if (params.containsKey(name) && !params.get(name).isEmpty()) {
            return Arrays.asList(params.get(name).split(","));
        }
        return new ArrayList<>();
    }
}
