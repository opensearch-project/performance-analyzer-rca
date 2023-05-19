/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.collectors.GCInfoCollector;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.reader_writer_shared.Event;

public class GarbageCollectorInfoProcessorTest {

    private static final String DB_URL = "jdbc:sqlite:";
    private static final String TEST_MEM_POOL = "testMemPool";
    private static final String COLLECTOR_NAME = "testCollectorName";
    private static final String GC_INFO_KEY = "gc_info";
    private GarbageCollectorInfoProcessor gcProcessor;
    private long currTimestamp;
    private NavigableMap<Long, GarbageCollectorInfoSnapshot> snapMap;
    Connection conn;

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
        this.currTimestamp = System.currentTimeMillis();
        this.snapMap = new TreeMap<>();
        this.gcProcessor =
                GarbageCollectorInfoProcessor.buildGarbageCollectorInfoProcessor(
                        currTimestamp, conn, snapMap);
    }

    @Test
    public void testHandleEvent() throws Exception {
        Event testEvent = buildTestGcInfoEvent();

        gcProcessor.initializeProcessing(currTimestamp, System.currentTimeMillis());

        assertTrue(gcProcessor.shouldProcessEvent(testEvent));

        gcProcessor.processEvent(testEvent);
        gcProcessor.finalizeProcessing();

        GarbageCollectorInfoSnapshot snap = snapMap.get(currTimestamp);
        Result<Record> result = snap.fetchAll();
        assertEquals(1, result.size());
        Assert.assertEquals(
                TEST_MEM_POOL,
                result.get(0).get(GarbageCollectorInfoSnapshot.Fields.MEMORY_POOL.toString()));
        Assert.assertEquals(
                COLLECTOR_NAME,
                result.get(0).get(GarbageCollectorInfoSnapshot.Fields.COLLECTOR_NAME.toString()));
    }

    private Event buildTestGcInfoEvent() {
        StringBuilder val = new StringBuilder();
        val.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        val.append(new GCInfoCollector.GCInfo(TEST_MEM_POOL, COLLECTOR_NAME).serialize())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        return new Event(GC_INFO_KEY, val.toString(), System.currentTimeMillis());
    }
}
