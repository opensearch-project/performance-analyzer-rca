/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metricsdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("serial")
public class MetricsDBTests {
    private MetricsDB db;
    private Long timestamp;

    @Before
    public void setUp() throws Exception {
        System.setProperty("java.io.tmpdir", "/tmp");
        timestamp = 1553713380L;
        this.db = new MetricsDB(timestamp);
    }

    public MetricsDBTests() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    @After
    public void tearDown() throws Exception {
        this.db.remove();
        deleteAll();
    }

    @Test
    public void testTableCreation() throws Exception {

        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);

        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "2", "ac-test");

        Iterable<Record> res =
                db.queryMetric(
                        Arrays.asList("cpu"),
                        Arrays.asList("sum"),
                        Arrays.asList("shard", "index"));
        for (Record r : res) {
            String s = r.get("shard").toString();
            assertTrue(s.equals("1"));
            String index = r.get("index").toString();
            assertTrue(index.equals("ac-test"));
            break;
        }

        res = db.queryMetric(Arrays.asList("cpu"), Arrays.asList("sum"), Arrays.asList());
        for (Record r : res) {
            Double sum = Double.parseDouble(r.get("cpu").toString());
            assertEquals(14D, sum.doubleValue(), 0);
        }
        db.close();
    }

    @Test
    public void testTableNonexistent() throws Exception {

        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);

        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "1", "ac-test");

        Result<Record> res =
                db.queryMetric(
                        Arrays.asList("pseudocpu"),
                        Arrays.asList("sum"),
                        Arrays.asList("shard", "index"));
        assertNull(res);

        res =
                db.queryMetric(
                        Arrays.asList("cpu", "pseudocpu"),
                        Arrays.asList("sum", "sum"),
                        Arrays.asList("shard", "index"));

        assertEquals(1, res.size());
        assertEquals(14D, Double.parseDouble(res.get(0).get("cpu").toString()), 0);
        assertNull(res.get(0).get("pseudocpu"));

        db.close();
    }

    @Test
    public void testMultiMetric() throws Exception {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        db.createMetric(Metric.rss(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "1", "ac-test");
        putRSSMetric(db, 1D, "1", "ac-test");
        putRSSMetric(db, 5D, "1", "ac-test");
        Iterable<Record> res =
                db.queryMetric(
                        Arrays.asList("cpu", "rss"),
                        Arrays.asList("sum", "sum"),
                        Arrays.asList("shard", "index"));
        for (Record r : res) {
            Double cpu = Double.parseDouble(r.get("cpu").toString());
            Double rss = Double.parseDouble(r.get("rss").toString());
            assertEquals(14D, cpu.doubleValue(), 0);
            assertEquals(6D, rss.doubleValue(), 0);
        }
        db.close();
    }

    @Test
    public void testMultiMetricOuterJoin() throws Exception {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        db.createMetric(Metric.rss(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 20D, "2", "ac-test");
        putRSSMetric(db, 1D, "1", "ac-test");
        putRSSMetric(db, 3D, "3", "ac-test");
        Result<Record> res =
                db.queryMetric(
                        Arrays.asList("cpu", "rss"),
                        Arrays.asList("sum", "sum"),
                        Arrays.asList("shard", "index"));

        assertEquals(3, res.size());
        boolean assert_cpu_only = false;
        boolean assert_rss_only = false;
        boolean assert_cpu_rss = false;
        for (int i = 0; i < res.size(); i++) {
            Record r = res.get(i);
            if (r.get("shard").toString().equals("1")) {
                assert_cpu_rss =
                        Double.parseDouble(r.get("cpu").toString()) == 10D
                                && Double.parseDouble(r.get("rss").toString()) == 1D;
            } else if (r.get("shard").toString().equals("2")) {
                assert_cpu_only = Double.parseDouble(r.get("cpu").toString()) == 20D;
            } else if (r.get("shard").toString().equals("3")) {
                assert_rss_only = Double.parseDouble(r.get("rss").toString()) == 3D;
            }
        }
        assertTrue(assert_cpu_only);
        assertTrue(assert_rss_only);
        assertTrue(assert_cpu_rss);
        db.close();
    }

    @Test
    public void testMultiMetricNoDimension() throws Exception {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        db.createMetric(Metric.rss(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 30D, "2", "ac-test");
        putRSSMetric(db, 1D, "1", "ac-test");
        putRSSMetric(db, 3D, "3", "ac-test");
        Result<Record> res =
                db.queryMetric(
                        Arrays.asList("cpu", "rss"), Arrays.asList("avg", "sum"), Arrays.asList());

        assertEquals(1, res.size());

        assertEquals(20D, Double.parseDouble(res.get(0).get("cpu").toString()), 0);
        assertEquals(4D, Double.parseDouble(res.get(0).get("rss").toString()), 0);
        db.close();
    }

    @Test
    public void testGroupBy() throws Exception {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        db.createMetric(Metric.rss(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "1", "ac-test");
        putCPUMetric(db, 6D, "2", "ac-test");
        putCPUMetric(db, 8D, "1", "ac-test-2");
        putRSSMetric(db, 1D, "1", "ac-test");
        putRSSMetric(db, 5D, "1", "ac-test-2");
        putRSSMetric(db, 3D, "2", "ac-test");
        putRSSMetric(db, 3D, "2", "ac-test");
        Result<Record> res =
                db.queryMetric(
                        Arrays.asList("cpu", "rss"),
                        Arrays.asList("sum", "sum"),
                        Arrays.asList("shard", "index"));
        assertEquals(3, res.size());
        Double cpu = Double.parseDouble(res.get(0).get("cpu").toString());
        Double rss = Double.parseDouble(res.get(0).get("rss").toString());
        assertEquals(14D, cpu.doubleValue(), 0);
        assertEquals(1D, rss.doubleValue(), 0);
        cpu = Double.parseDouble(res.get(1).get("cpu").toString());
        rss = Double.parseDouble(res.get(1).get("rss").toString());
        assertEquals(8D, cpu.doubleValue(), 0);
        assertEquals(5D, rss.doubleValue(), 0);
        cpu = Double.parseDouble(res.get(2).get("cpu").toString());
        rss = Double.parseDouble(res.get(2).get("rss").toString());
        assertEquals(6D, cpu.doubleValue(), 0);
        assertEquals(6D, rss.doubleValue(), 0);
    }

    @Test
    public void testAggAvg() throws Exception {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "1", "ac-test");
        Result<Record> res =
                db.queryMetric(
                        Arrays.asList("cpu"),
                        Arrays.asList("avg"),
                        Arrays.asList("shard", "index"));
        Double cpu = Double.parseDouble(res.get(0).get("cpu").toString());
        assertEquals(7D, cpu, 0);
    }

    private void putCPUMetric(MetricsDB db, Double val, String shard, String index) {
        Dimensions dimensions = new Dimensions();
        dimensions.put("shard", shard);
        dimensions.put("index", index);
        db.putMetric(Metric.cpu(val), dimensions, 0);
    }

    private void putRSSMetric(MetricsDB db, Double val, String shard, String index) {
        Dimensions dimensions = new Dimensions();
        dimensions.put("shard", shard);
        dimensions.put("index", index);
        db.putMetric(Metric.rss(val), dimensions, 0);
    }

    // @Test
    public void perfTestSingleThread() throws Exception {
        System.out.println("Batch Insert");
        long mCurrT = System.currentTimeMillis();
        // System.out.println("100000: "+runBatchTest(100000, "cpu"));
        for (int i = 0; i < 5; i++) {
            System.out.println("100000: cpu " + runBatchTest(100000, "cpu", db));
            System.out.println("100000: rss " + runBatchTest(100000, "rss", db));
            System.out.println("100000: paging " + runBatchTest(100000, "paging", db));
        }
        long mFinalT = System.currentTimeMillis();
        db.commit();
        System.out.println("Total time taken: " + (mFinalT - mCurrT));
        // Thread.sleep(1000);
    }

    // @Test
    public void perfTest() throws Exception {
        System.out.println("Batch Insert");
        long mCurrT = System.currentTimeMillis();
        // System.out.println("100000: "+runBatchTest(100000, "cpu"));
        for (int i = 0; i < 5; i++) {
            Thread t1 =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        System.out.println(
                                                "100000: cpu " + runBatchTest(100000, "cpu", db));
                                    } catch (Exception e) {
                                        System.out.println("Exception hit");
                                        System.out.println(e);
                                    }
                                }
                            });

            Thread t2 =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        System.out.println(
                                                "100000: rss " + runBatchTest(100000, "rss", db));
                                    } catch (Exception e) {
                                        System.out.println("Exception hit");
                                        System.out.println(e);
                                    }
                                }
                            });

            Thread t3 =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        System.out.println(
                                                "100000: paging "
                                                        + runBatchTest(100000, "paging", db));
                                    } catch (Exception e) {
                                        System.out.println("Exception hit");
                                        System.out.println(e);
                                    }
                                }
                            });

            t2.start();
            t3.start();
            t1.start();
            t1.join();
            t2.join();
            t3.join();
        }
        long mFinalT = System.currentTimeMillis();
        db.commit();
        System.out.println("Total time taken: " + (mFinalT - mCurrT));
    }

    private Long runBatchTest(int iterations, String metricColumn, MetricsDB mdb) throws Exception {
        long mCurrT = System.currentTimeMillis();

        List<String> dims =
                new ArrayList<String>() {
                    {
                        this.add("shard");
                        this.add("index");
                        this.add("operation");
                        this.add("role");
                    }
                };
        mdb.createMetric(new Metric<Double>(metricColumn, 0d), dims);
        BatchBindStep handle = mdb.startBatchPut(new Metric<Double>(metricColumn, 0d), dims);

        Dimensions dimensions = new Dimensions();
        for (int i = 0; i < iterations; i++) {
            handle.bind("shard", "index", "operation", "role", 12345L);
        }
        handle.execute();
        long mFinalT = System.currentTimeMillis();
        return mFinalT - mCurrT;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectTableName() {
        this.db.startBatchPut("", 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectDimensionNum() {
        this.db.startBatchPut("", 0);
    }

    @Test
    public void testFetchExisting_exists() throws Exception {
        MetricsDB existing = MetricsDB.fetchExisting(timestamp);
        existing.remove();
    }

    @Test(expected = Exception.class)
    public void testFetchExisting_notExists() throws Exception {
        MetricsDB existing = MetricsDB.fetchExisting(timestamp + 1);
    }

    @Test
    public void testQueryMetric_invalidMetric() {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "2", "ac-test");

        assertNull(db.queryMetric("rss", Arrays.asList("shard"), 1));
    }

    @Test(expected = Exception.class)
    public void testQueryMetric_invalidLimit() {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "2", "ac-test");

        Result<Record> res = db.queryMetric("cpu", Arrays.asList("shard"), -1);
    }

    @Test(expected = Exception.class)
    public void testQueryMetric_invalidDimension() {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "2", "ac-test");

        Result<Record> res = db.queryMetric("cpu", Arrays.asList("operation"), 1);
    }

    @Test
    public void testQueryMetric_basic() {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "2", "ac-test");

        Iterator<Record> res = db.queryMetric("cpu", columns, 2).iterator();
        Record r = res.next();
        assertTrue(r.get("shard").toString().equals("1"));
        assertTrue(r.get("index").toString().equals("ac-test"));
        assertTrue(res.hasNext());
        r = res.next();
        assertTrue(r.get("shard").toString().equals("2"));
        assertTrue(r.get("index").toString().equals("ac-test"));
        assertFalse(res.hasNext());
    }

    @Test
    public void testQueryMetric_limited() {
        List<String> columns = Arrays.asList("shard", "index");
        db.createMetric(Metric.cpu(10D), columns);
        putCPUMetric(db, 10D, "1", "ac-test");
        putCPUMetric(db, 4D, "2", "ac-test");

        Iterator<Record> res = db.queryMetric("cpu", columns, 1).iterator();
        Record r = res.next();
        assertTrue(r.get("shard").toString().equals("1"));
        assertTrue(r.get("index").toString().equals("ac-test"));
        assertFalse(res.hasNext());
    }

    @Test
    public void testDeleteOnDiskFile_exists() throws Exception {
        long timestamp = 1000000000;
        MetricsDB existing = new MetricsDB(timestamp);
        existing.remove();
        MetricsDB.deleteOnDiskFile(timestamp);
        assertFalse(new File(existing.getDBFilePath()).exists());
    }

    @Test
    public void testDeleteOnDiskFile_notExists() throws Exception {
        long timestamp = 1000000000;
        MetricsDB existing = new MetricsDB(timestamp);
        existing.remove();
        MetricsDB.deleteOnDiskFile(timestamp);
        assertFalse(new File(existing.getDBFilePath()).exists());
        MetricsDB.deleteOnDiskFile(timestamp);
        assertFalse(new File(existing.getDBFilePath()).exists());
    }

    @Test
    public void testListOnDiskFiles_empty() throws Exception {
        deleteAll();
        assertEquals(Collections.EMPTY_SET, MetricsDB.listOnDiskFiles());
    }

    @Test
    public void testListOnDiskFiles_one() throws Exception {
        deleteAll();
        Set<Long> expected = ImmutableSet.of(1000000000L);
        for (Long ts : expected) {
            (new MetricsDB(ts)).remove();
        }
        assertEquals(expected, MetricsDB.listOnDiskFiles());
    }

    @Test
    public void testListOnDiskFiles_two() throws Exception {
        deleteAll();
        Set<Long> expected = ImmutableSet.of(1000000000L, 500L);
        for (Long ts : expected) {
            (new MetricsDB(ts)).remove();
        }
        assertEquals(expected, MetricsDB.listOnDiskFiles());
    }

    @Test
    public void testListOnDiskFiles_many() throws Exception {
        deleteAll();
        Set<Long> expected = ImmutableSet.of(1000000000L, 500L, 0L);
        for (Long ts : expected) {
            (new MetricsDB(ts)).remove();
        }
        assertEquals(expected, MetricsDB.listOnDiskFiles());
    }

    public static void deleteAll() {
        final File folder = new File("/tmp");
        final File[] files =
                folder.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(final File dir, final String name) {
                                return name.matches("metricsdb_.*");
                            }
                        });
        for (final File file : files) {
            if (!file.delete()) {
                System.err.println("Can't remove " + file.getAbsolutePath());
            }
        }
    }
}
