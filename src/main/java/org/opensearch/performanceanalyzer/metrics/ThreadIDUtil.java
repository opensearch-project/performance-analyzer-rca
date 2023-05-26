/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics;


import org.opensearch.performanceanalyzer.commons.jvm.ThreadList;

public final class ThreadIDUtil {
    private ThreadIDUtil() {}

    public static final ThreadIDUtil INSTANCE = new ThreadIDUtil();

    public long getNativeCurrentThreadId() {

        return getNativeThreadId(Thread.currentThread().getId());
    }

    public long getNativeThreadId(long jTid) {
        ThreadList.ThreadState threadState1 = ThreadList.getThreadState(jTid);

        long nid = -1;
        if (threadState1 != null) {
            nid = threadState1.nativeTid;
        }

        return nid;
    }
}
