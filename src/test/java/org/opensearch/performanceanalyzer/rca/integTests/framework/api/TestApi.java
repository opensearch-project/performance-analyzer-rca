/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.rca.integTests.framework.Cluster;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;

/**
 * This is API class whose object is injected into each of the test methods in case test class
 * declares a @{code setTestApi(final TestApi api)}.
 */
public class TestApi {
    private static final Logger LOG = LogManager.getLogger(TestApi.class);

    /**
     * An instance of the cluster object to get access to the nodes to query various data to see
     * that the tests have the desired results.
     */
    private final Cluster cluster;

    public TestApi(Cluster cluster) {
        this.cluster = cluster;
    }

    public JsonElement getRcaDataOnHost(HostTag hostTag, String rcaName) {
        return cluster.getAllRcaDataOnHost(hostTag, rcaName);
    }

    public <T> Object constructObjectFromDBOnHost(HostTag hostTag, Class<T> clz) throws Exception {
        return cluster.constructObjectFromDBOnHost(hostTag, clz);
    }

    /**
     * This let's you make a REST request to the REST endpoint of a particular host identified by
     * the host tag.
     *
     * @param params the key value map that is passes as the request parameter.
     * @param hostByTag The host whose rest endpoint we will hit.
     * @return The response serialized as a String.
     */
    public JsonElement getRestResponse(
            @Nonnull final String queryUrl,
            @Nonnull final Map<String, String> params,
            HostTag hostByTag) {
        Objects.requireNonNull(params);
        JsonElement json =
                JsonParser.parseString(cluster.getRcaRestResponse(queryUrl, params, hostByTag));
        return json;
    }

    /**
     * This can be used to get all the records from all the tables in a host. This can be used for
     * validation of what gets persisted in the rca.sqlite tables.
     *
     * @param hostTag The host whose rca.sqlite will be queried.
     * @return A list of all the data stored in all the tables in the particular host.
     */
    public Map<String, Result<Record>> getRecordsForAllTablesOnHost(HostTag hostTag) {
        return cluster.getRecordsForAllTablesOnHost(hostTag);
    }

    /**
     * This API let's a gauntlet test writer swap out the metricsDB for a new one.
     *
     * @param clz The class whose AMetric@ should be used to replace it
     * @throws Exception Throws Exception
     */
    public void updateMetrics(Class<?> clz) throws Exception {
        updateMetrics(clz, false);
    }

    /**
     * This API let's a gauntlet test writer swap out the metricsDB for a new one.
     *
     * @param clz The class whose AMetric@ should be used to replace it
     * @param reloadDB whether to refresh entire DB or update tables in existing DB
     * @throws Exception Throws Exception
     */
    public void updateMetrics(Class<?> clz, boolean reloadDB) throws Exception {
        if (clz.isAnnotationPresent(AMetric.Metrics.class)
                || clz.isAnnotationPresent(AMetric.class)) {
            cluster.updateMetricsDB(clz.getAnnotationsByType(AMetric.class), reloadDB);
        }
    }
}
