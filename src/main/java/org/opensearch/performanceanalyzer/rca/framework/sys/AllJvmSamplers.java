/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.sys;


import java.util.Arrays;
import java.util.List;
import org.opensearch.performanceanalyzer.commons.stats.emitters.ISampler;

public class AllJvmSamplers {
    public static List<ISampler> getJvmSamplers() {
        return Arrays.asList(new JvmFreeMem(), new JvmTotalMem());
    }
}
