/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.overrides;


import java.util.HashMap;
import java.util.Map;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;

public class RcaConfIt extends RcaConf {
    private String rcaDataStorePath;
    private String rcaAnalysisGraph;

    public RcaConfIt(RcaConf rcaConf) {
        super(rcaConf.getConfigFileLoc());
    }

    public void setRcaDataStorePath(String dataStorePath) {
        this.rcaDataStorePath = dataStorePath;
    }

    @Override
    public Map<String, String> getDatastore() {
        Map<String, String> map = new HashMap<>(super.getDatastore());
        map.put(RcaConsts.DATASTORE_LOC_KEY, rcaDataStorePath);
        return map;
    }

    @Override
    public String getAnalysisGraphEntryPoint() {
        return rcaAnalysisGraph;
    }
}
