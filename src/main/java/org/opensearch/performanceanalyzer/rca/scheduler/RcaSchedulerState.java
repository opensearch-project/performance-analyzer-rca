/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.scheduler;

public enum RcaSchedulerState {
    STATE_NOT_STARTED,
    STATE_STARTED,
    STATE_STOPPED,
    STATE_STOPPED_DUE_TO_EXCEPTION
}
