/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.jvm;


import org.junit.Test;
import org.opensearch.performanceanalyzer.hwnet.NetworkInterface;
import org.opensearch.performanceanalyzer.os.OSGlobals;

public class ThreadListTests {
    // XXX: standalone test code
    public static class HelloRunnable implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("duMMy-thread");
            long i = 0;
            while (true) {
                synchronized (HelloRunnable.class) {
                    String.valueOf(i++);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
        (new Thread(new HelloRunnable())).start();
        (new Thread(new HelloRunnable())).start();
        runOnce();
    }

    private static void runOnce() throws InterruptedException {
        String params[] = new String[0];
        while (true) {
            ThreadList.runThreadDump(OSGlobals.getPid(), params);
            ThreadList.LOGGER.info(ThreadList.getNativeTidMap().values());

            /*GCMetrics.runOnce();
            HeapMetrics.runOnce();
            ThreadCPU.runOnce();
            ThreadDiskIO.runOnce();
            ThreadSched.runOnce();
            NetworkE2E.runOnce();
            Disks.runOnce();*/
            NetworkInterface.runOnce();

            Thread.sleep(ThreadList.samplingInterval);
        }
    }

    // - to enhance
    @Test
    public void testMetrics() {}
}
