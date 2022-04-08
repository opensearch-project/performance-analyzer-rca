/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.jvm;


import com.sun.tools.attach.VirtualMachine;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.OSMetricsGeneratorFactory;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.collectors.ScheduledMetricCollectorsExecutor;
import org.opensearch.performanceanalyzer.core.Util;
import org.opensearch.performanceanalyzer.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.rca.framework.metrics.WriterMetrics;
import sun.tools.attach.HotSpotVirtualMachine;

/** Traverses and prints the stack traces for all Java threads in the remote VM */
public class ThreadList {
    private static final Map<Long, String> jTidNameMap = new ConcurrentHashMap<>();
    private static final Map<Long, ThreadState> nativeTidMap = new ConcurrentHashMap<>();
    private static final Map<Long, ThreadState> oldNativeTidMap = new ConcurrentHashMap<>();
    private static final Map<Long, ThreadState> jTidMap = new ConcurrentHashMap<>();
    private static final Map<String, ThreadState> nameMap = new ConcurrentHashMap<>();
    private static final String pid = OSMetricsGeneratorFactory.getInstance().getPid();
    static final Logger LOGGER = LogManager.getLogger(ThreadList.class);
    static final int samplingInterval =
            MetricsConfiguration.CONFIG_MAP.get(ThreadList.class).samplingInterval;

    // This value controls how often we do the thread dump.
    private static final long minRunInterval = samplingInterval;
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final Pattern linePattern = Pattern.compile("\"([^\"]*)\"");
    private static long lastRunTime = 0;

    private static Lock vmAttachLock = new ReentrantLock();

    public static class ThreadState {
        public long javaTid;
        public long nativeTid;
        public long heapUsage;
        public String threadName;
        public String tState;
        public Thread.State state;
        public long blockedCount;
        public long blockedTime;
        public long waitedCount;
        public long waitedTime;

        public double heapAllocRate;
        public double avgBlockedTime;
        public double avgWaitedTime;

        ThreadState() {
            javaTid = -1;
            nativeTid = -1;
            heapUsage = -1;
            heapAllocRate = 0;
            blockedCount = 0;
            blockedTime = 0;
            waitedCount = 0;
            waitedTime = 0;
            avgBlockedTime = 0;
            avgWaitedTime = 0;
            threadName = "";
            tState = "";
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("javatid:")
                    .append(javaTid)
                    .append(" nativetid:")
                    .append(nativeTid)
                    .append(" name:")
                    .append(threadName)
                    .append(" state:")
                    .append(tState)
                    .append("(")
                    .append(state)
                    .append(")")
                    .append(" heaprate: ")
                    .append(heapAllocRate)
                    .append(" bTime: ")
                    .append(avgBlockedTime)
                    .append(":")
                    .append(blockedCount)
                    .append(" wTime: ")
                    .append(avgWaitedTime)
                    .append(":")
                    .append(waitedCount)
                    .toString();
        }
    }

    /**
     * This is called from OSMetricsCollector#collectMetrics. So this is not called in the critical
     * path of OpenSearch request handling. Even for the collector thread, we do a timed wait to
     * acquire this lock and move on if we could not get it.
     *
     * @return A hashmap of threadId to threadState.
     * @param threadContentionMonitoringEnabled
     */
    public static Map<Long, ThreadState> getNativeTidMap(
            boolean threadContentionMonitoringEnabled) {
        if (threadBean.isThreadContentionMonitoringSupported()) {
            threadBean.setThreadContentionMonitoringEnabled(threadContentionMonitoringEnabled);
        }
        if (vmAttachLock.tryLock()) {
            try {
                // Thread dumps are expensive and therefore we make sure that at least
                // minRunInterval milliseconds have elapsed between two attempts.
                if (System.currentTimeMillis() > lastRunTime + minRunInterval) {
                    runThreadDump(pid, new String[0]);
                }
            } finally {
                vmAttachLock.unlock();
            }
        } else {
            PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                    WriterMetrics.JVM_ATTACH_LOCK_ACQUISITION_FAILED, "", 1);
        }

        // - sending a copy so that if runThreadDump next iteration clears it; caller still has the
        // state at the call time
        // - not too expensive as this is only being called from Scheduled Collectors (only once in
        // few seconds)
        return new HashMap<>(nativeTidMap);
    }

    /**
     * This method is called from the critical bulk and search paths which PA intercepts. This
     * method used to try to do a thread dump if it could not find the information about the thread
     * in question. The thread dump is an expensive operation and can stall see
     * VirtualMachineImpl#VirtualMachineImpl() for jdk-11 u06. We don't want the OpenSearch threads
     * to pay the price. We skip this iteration and then hopefully in the next call to
     * getNativeTidMap(), the OSMetricsCollector#collectMetrics will fill the jTidMap. This
     * transfers the responsibility from the OpenSearch threads to the PA collector threads.
     *
     * @param threadId The threadId of the current thread.
     * @return If we have successfully captured the ThreadState, then we emit it or Null otherwise.
     */
    public static ThreadState getThreadState(long threadId) {
        ThreadState retVal = jTidMap.get(threadId);
        if (retVal == null) {
            PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                    WriterMetrics.NO_THREAD_STATE_INFO, "", 1);
        }
        return retVal;
    }

    // Attach to pid and perform a thread dump
    private static void runAttachDump(String pid, String[] args) {
      
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(pid);
        } catch (Exception ex) {
            if (ex.getMessage().contains("java_pid")) {
                PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                        WriterMetrics.JVM_ATTACH_ERROR_JAVA_PID_FILE_MISSING, "", 1);
            } else {
                PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                        WriterMetrics.JVM_ATTACH_ERROR, "", 1);
            }
            // If the thread dump failed then we clean up the old map. So, next time when the
            // collection
            // happens as it would after a bootup.
            oldNativeTidMap.clear();
            return;
        }

        try (InputStream in = ((HotSpotVirtualMachine) vm).remoteDataDump(args); ) {
            createMap(in);
        } catch (Exception ex) {
            PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                    WriterMetrics.JVM_ATTACH_ERROR, "", 1);
            oldNativeTidMap.clear();
        }

        try {
            vm.detach();
            PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                    WriterMetrics.JVM_THREAD_DUMP_SUCCESSFUL, "", 1);
        } catch (Exception ex) {
            PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                    WriterMetrics.JVM_ATTACH_ERROR, "", 1);
        }
    }

    public static void parseAllThreadInfos(ThreadInfo[] infos) {
        for (ThreadInfo info : infos) {
            try {
                parseThreadInfo(info);
            } catch (Exception ex) {
                // If the ids provided to the getThreadInfo() call are not valid ids or the threads
                // no
                // longer exists, then the corresponding info object will contain null.
                PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                        WriterMetrics.JVM_THREAD_ID_NO_LONGER_EXISTS, "", 1);
            }
        }
    }

    public static ThreadInfo[] getAllThreadInfos() {
        long[] ids = threadBean.getAllThreadIds();
        return threadBean.getThreadInfo(ids);
    }

    // ThreadMXBean-based info for tid, name and allocs
    private static void runMXDump() {
        ThreadInfo[] infos = getAllThreadInfos();
        parseAllThreadInfos(infos);
        ThreadHistory.cleanup();
    }

    private static void parseThreadInfo(final ThreadInfo info) {
        long id = info.getThreadId();
        String name = info.getThreadName();
        Thread.State state = info.getThreadState();

        // following captures cumulative allocated bytes + TLAB used bytes
        // and it is cumulative
        long mem = ((com.sun.management.ThreadMXBean) threadBean).getThreadAllocatedBytes(id);

        ThreadState t = jTidMap.get(id);
        if (t == null) {
            return;
        }
        t.heapUsage = mem;
        t.state = state;
        t.blockedCount = info.getBlockedCount();
        t.blockedTime = info.getBlockedTime();
        t.waitedCount = info.getWaitedCount();
        t.waitedTime = info.getWaitedTime();
        ThreadHistory.addBlocked(
                t.nativeTid, (state == Thread.State.BLOCKED) ? samplingInterval : 0);
        ThreadHistory.addWaited(
                t.nativeTid,
                (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING)
                        ? samplingInterval
                        : 0);

        long curRunTime = System.currentTimeMillis();
        ThreadState oldt = oldNativeTidMap.get(t.nativeTid);
        if (curRunTime > lastRunTime && oldt != null) {
            t.heapAllocRate =
                    Math.max(t.heapUsage - oldt.heapUsage, 0) * 1.0e3 / (curRunTime - lastRunTime);
            if (t.blockedTime != -1 && t.blockedCount > oldt.blockedCount) {
                t.avgBlockedTime =
                        1.0e-3
                                * (t.blockedTime - oldt.blockedTime)
                                / (t.blockedCount - oldt.blockedCount);
            } else if (t.blockedCount == oldt.blockedCount && t.blockedTime > oldt.blockedTime) {
                t.avgBlockedTime =
                        1.0e-3 * (t.blockedTime - oldt.blockedTime + oldt.avgBlockedTime);
            } else {
                CircularLongArray arr = ThreadHistory.blockedTidHistoryMap.get(t.nativeTid);
                // NOTE: this is an upper bound
                if (arr != null) {
                    t.avgBlockedTime = 1.0 * arr.getAvgValue() / samplingInterval;
                }
            }
            if (t.waitedTime != -1 && t.waitedCount > oldt.waitedCount) {
                t.avgWaitedTime =
                        1.0e-3
                                * (t.waitedTime - oldt.waitedTime)
                                / (t.waitedCount - oldt.waitedCount);
            } else if (t.waitedCount == oldt.waitedCount && t.waitedTime > oldt.waitedTime) {
                t.avgWaitedTime = 1.0e-3 * (t.waitedTime - oldt.waitedTime + oldt.avgWaitedTime);
            } else {
                CircularLongArray arr = ThreadHistory.waitedTidHistoryMap.get(t.nativeTid);
                // NOTE: this is an upper bound
                if (arr != null) {
                    t.avgWaitedTime = 1.0 * arr.getAvgValue() / samplingInterval;
                }
            }
        }
        jTidNameMap.put(id, name);
    }

    static void runThreadDump(String pid, String[] args) {
        String currentThreadName = Thread.currentThread().getName();
        assert currentThreadName.startsWith(
                                ScheduledMetricCollectorsExecutor.COLLECTOR_THREAD_POOL_NAME)
                        || currentThreadName.equals(
                                ScheduledMetricCollectorsExecutor.class.getSimpleName())
                : String.format(
                        "Thread dump called from a non os collector thread: %s", currentThreadName);
        jTidNameMap.clear();
        oldNativeTidMap.putAll(nativeTidMap);
        nativeTidMap.clear();
        jTidMap.clear();
        nameMap.clear();

        // TODO: make this map update atomic
        Util.invokePrivileged(() -> runAttachDump(pid, args));
        // oldNativeTidMap gets cleared if the attach Fails, so that the
        // metrics collection starts as it would after a restart.
        if (!oldNativeTidMap.isEmpty()) {
            runMXDump();
        }
        lastRunTime = System.currentTimeMillis();
    }

    private static void parseLine(String line) {
        String[] tokens = line.split(" os_prio=[0-9]* ");
        ThreadState t = new ThreadState();
        t.javaTid = -1;

        Matcher m = linePattern.matcher(tokens[0]);
        if (!m.find()) {
            t.threadName = tokens[0];
        } else {
            t.threadName = m.group(1);
            if (!tokens[0].equals("\"" + t.threadName + "\"")) {
                t.javaTid =
                        Long.parseLong(
                                tokens[0]
                                        .split(Pattern.quote("\"" + t.threadName + "\" "))[1]
                                        .split(" ")[0]
                                        .split("#")[1]);
            }
        }

        tokens = tokens[1].split(" ");
        for (String token : tokens) {
            String[] keyValuePare = token.split("=");
            if (keyValuePare.length < 2) {
                continue;
            }
            if (t.javaTid == -1 && keyValuePare[0].equals("tid")) {
                t.javaTid = Long.decode(keyValuePare[1]);
            }
            if (keyValuePare[0].equals("nid")) {
                t.nativeTid = Long.decode(keyValuePare[1]);
            }
        }
        t.tState = tokens[2]; // TODO: stuff like "in Object.wait()"
        nativeTidMap.put(t.nativeTid, t);
        jTidMap.put(t.javaTid, t);
        nameMap.put(t.threadName, t); // XXX: we assume no collisions
    }

    private static void createMap(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while ((line = br.readLine()) != null) {
            if (line.contains("tid=")) {
                parseLine(line);
            }
        }
    }

    // currently stores thread states to track locking periods
    static class ThreadHistory {
        public static Map<Long, CircularLongArray> blockedTidHistoryMap = new HashMap<>();
        public static Map<Long, CircularLongArray> waitedTidHistoryMap = new HashMap<>();
        private static final int HISTORY_SIZE = 60; // 60 * samplingInterval

        public static void addBlocked(long tid, long value) {
            add(tid, value, blockedTidHistoryMap);
        }

        public static void addWaited(long tid, long value) {
            add(tid, value, waitedTidHistoryMap);
        }

        public static void cleanup() {
            long curTime = System.currentTimeMillis();
            cleanUp(curTime, blockedTidHistoryMap);
            cleanUp(curTime, waitedTidHistoryMap);
        }

        private static void add(long tid, long value, Map<Long, CircularLongArray> tidHistoryMap) {
            CircularLongArray arr = tidHistoryMap.get(tid);
            if (arr == null) {
                arr = new CircularLongArray(HISTORY_SIZE);
                arr.add(value);
                tidHistoryMap.put(tid, arr);
            } else {
                arr.add(value);
            }
        }

        private static void cleanUp(long curTime, Map<Long, CircularLongArray> tidHistoryMap) {
            for (Iterator<Map.Entry<Long, CircularLongArray>> it =
                            tidHistoryMap.entrySet().iterator();
                    it.hasNext(); ) {
                Map.Entry<Long, CircularLongArray> me = it.next();
                CircularLongArray arr = me.getValue();
                // delete items updated older than 300s
                if (curTime - arr.lastWriteTimestamp > HISTORY_SIZE * samplingInterval * 1.0e3) {
                    it.remove();
                }
            }
        }
    }

    // models a fixed-capacity queue that is append-only
    // not thread-safe
    static class CircularLongArray {
        ArrayList<Long> list = null;
        public long lastWriteTimestamp;
        private long totalValue;
        private int startidx;
        private int capacity;

        CircularLongArray(int capacity) {
            list = new ArrayList<>(capacity);
            this.capacity = capacity;
            totalValue = 0;
            startidx = 0;
            lastWriteTimestamp = 0;
        }

        public boolean add(long e) {
            lastWriteTimestamp = System.currentTimeMillis();
            if (list.size() < capacity) {
                // can only happen if startidx == 0
                if (startidx != 0) {
                    return false;
                } else {
                    totalValue += e;
                    return list.add(e);
                }
            }
            totalValue -= list.get(startidx);
            totalValue += e;
            list.set(startidx, e);
            startidx = (startidx + 1) % capacity;
            return true;
        }

        public double getAvgValue() {
            return list.size() == 0 ? 0 : 1.0 * totalValue / list.size();
        }
    }
}
