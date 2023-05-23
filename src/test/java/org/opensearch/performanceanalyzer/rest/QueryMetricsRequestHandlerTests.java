/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rest;

import static org.junit.Assert.assertEquals;

import io.grpc.stub.StreamObserver;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.grpc.MetricsResponse;
import org.opensearch.performanceanalyzer.metrics.MetricsRestUtil;
import org.opensearch.performanceanalyzer.metrics.handler.MetricsServerHandler;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;

@SuppressWarnings("serial")
public class QueryMetricsRequestHandlerTests {
    MetricsRestUtil metricsRestUtil;

    public QueryMetricsRequestHandlerTests() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
    }

    @Before
    public void createObject() {
        this.metricsRestUtil = new MetricsRestUtil();
    }

    @Test
    public void testNodeJsonBuilder() throws Exception {
        String rootLocation = "test_files/dev/shm";
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);
        ConcurrentHashMap<String, String> nodeResponses =
                new ConcurrentHashMap<String, String>() {
                    {
                        this.put("node1", "{'xyz':'abc'}");
                        this.put("node2", "{'xyz':'abc'}");
                    }
                };
        assertEquals(
                "{\"node2\": {'xyz':'abc'}, \"node1\" :{'xyz':'abc'}}",
                metricsRestUtil.nodeJsonBuilder(nodeResponses));
    }

    // Disabled on purpose
    // @Test
    public void testQueryJson() throws Exception {
        String rootLocation = "build/private/test_resources/dev/shm";
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);
        mp.processMetrics(rootLocation, 1535065139000L);
        mp.processMetrics(rootLocation, 1535065169000L);
        mp.processMetrics(rootLocation, 1535065199000L);
        mp.processMetrics(rootLocation, 1535065229000L);
        mp.processMetrics(rootLocation, 1535065259000L);
        mp.processMetrics(rootLocation, 1535065289000L);
        mp.processMetrics(rootLocation, 1535065319000L);
        mp.processMetrics(rootLocation, 1535065349000L);
        MetricsServerHandler serviceHandler = new MetricsServerHandler();

        StreamObserver<MetricsResponse> responseObserver =
                new StreamObserver<MetricsResponse>() {
                    String response = "";

                    @Override
                    public void onNext(MetricsResponse value) {
                        response = value.getMetricsResult();
                    }

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onCompleted() {
                        assertEquals(
                                "{\"timestamp\": 1234, \"data\": {\"fields\":[{\"name\":"
                                        + "\"ShardID\",\"type\":\"VARCHAR\"},{\"name\":\"IndexName\","
                                        + "\"type\":\"VARCHAR\"},{\"name\":\"Operation\",\"type\":"
                                        + "\"VARCHAR\"},{\"name\":\"CPU_Utilization\",\"type\":\"DOUBLE\""
                                        + "}],\"records\":[[null,null,\"GC\",0.0],[null,null,\"management\",0.0],[null,null,\"other\""
                                        + ",0.0256],[null,null,\"refresh\",0.0],[\"0\",\"sonested\",\"shardfetch\",0.00159186808056345],"
                                        + "[\"0\",\"sonested\",\"shardquery\",1.55800813191944]]}}",
                                response);
                    }
                };
        serviceHandler.collectStats(
                mp.getMetricsDB().getValue(),
                1234L,
                Arrays.asList(AllMetrics.OSMetrics.CPU_UTILIZATION.toString()),
                Arrays.asList("sum"),
                Arrays.asList(
                        AllMetrics.CommonDimension.SHARD_ID.toString(),
                        AllMetrics.CommonDimension.INDEX_NAME.toString(),
                        AllMetrics.CommonDimension.OPERATION.toString()),
                responseObserver);
    }

    @Test
    public void testParseArrayParameter() throws Exception {
        String rootLocation = "test_files/dev/shm";
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("metrics", "cpu");

        List<String> ret = metricsRestUtil.parseArrayParam(params, "metrics", false);
        assertEquals(1, ret.size());
        assertEquals("cpu", ret.get(0));

        params.put("metrics", "cpu,rss");

        ret = metricsRestUtil.parseArrayParam(params, "metrics", false);
        assertEquals(2, ret.size());
        assertEquals("cpu", ret.get(0));
        assertEquals("rss", ret.get(1));
    }

    @Test
    public void testParseArrayParameterOptional() throws Exception {
        String rootLocation = "test_files/dev/shm";
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);

        HashMap<String, String> params = new HashMap<String, String>();
        List<String> ret = metricsRestUtil.parseArrayParam(params, "metrics", true);
        assertEquals(0, ret.size());

        params.put("metrics", "");
        ret = metricsRestUtil.parseArrayParam(params, "metrics", true);
        assertEquals(0, ret.size());
    }

    @Test(expected = InvalidParameterException.class)
    public void testParseArrayParameterNoParam() throws Exception {
        String rootLocation = "test_files/dev/shm";
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);

        HashMap<String, String> params = new HashMap<String, String>();
        List<String> ret = metricsRestUtil.parseArrayParam(params, "metrics", false);
    }

    @Test(expected = InvalidParameterException.class)
    public void testParseArrayParameterEmptyParam() throws Exception {
        String rootLocation = "test_files/dev/shm";
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("metrics", "");
        List<String> ret = metricsRestUtil.parseArrayParam(params, "metrics", false);
    }
}
