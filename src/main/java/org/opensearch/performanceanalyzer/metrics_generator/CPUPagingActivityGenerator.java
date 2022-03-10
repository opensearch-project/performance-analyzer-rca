/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;

public interface CPUPagingActivityGenerator {

    // This method will be called before all following get methods
    // to make sure that all information exists for a thread id
    boolean hasPagingActivity(String threadId);

    double getCPUUtilization(String threadId);

    double getMajorFault(String threadId);

    double getMinorFault(String threadId);

    double getResidentSetSize(String threadId);

    void addSample();
}
