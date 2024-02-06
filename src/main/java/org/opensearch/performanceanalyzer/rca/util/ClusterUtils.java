/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.util;

import java.util.List;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

/** Utility class to get details about the nodes in the cluster. */
public class ClusterUtils {
    public static boolean isHostIdInCluster(
            final InstanceDetails.Id hostId, final List<InstanceDetails> clusterInstances) {
        return clusterInstances.stream().anyMatch(x -> hostId.equals(x.getInstanceId()));
    }
}
