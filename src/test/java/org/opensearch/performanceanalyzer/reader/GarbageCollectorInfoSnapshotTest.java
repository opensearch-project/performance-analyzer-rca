/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GarbageCollectorInfoSnapshotTest {

    private static final String DB_URL = "jdbc:sqlite:";
    private static final String TEST_MEM_POOL = "OldGen";
    private static final String COLLECTOR_NAME = "G1";
    private Connection conn;
    GarbageCollectorInfoSnapshot snapshot;

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
        snapshot = new GarbageCollectorInfoSnapshot(conn, System.currentTimeMillis());
    }

    @Test
    public void testReadGcSnapshot() throws Exception {
        final BatchBindStep handle = snapshot.startBatchPut();
        insertIntoTable(handle, TEST_MEM_POOL, COLLECTOR_NAME);

        final Result<Record> result = snapshot.fetchAll();

        assertEquals(1, result.size());
        Assert.assertEquals(
                TEST_MEM_POOL,
                result.get(0).get(GarbageCollectorInfoSnapshot.Fields.MEMORY_POOL.toString()));
        Assert.assertEquals(
                COLLECTOR_NAME,
                result.get(0).get(GarbageCollectorInfoSnapshot.Fields.COLLECTOR_NAME.toString()));
    }

    @After
    public void tearDown() throws Exception {
        conn.close();
    }

    private void insertIntoTable(BatchBindStep handle, String memPool, String collectorName) {
        Object[] bindVals = new Object[2];
        bindVals[0] = memPool;
        bindVals[1] = collectorName;
        handle.bind(bindVals).execute();
    }
}
