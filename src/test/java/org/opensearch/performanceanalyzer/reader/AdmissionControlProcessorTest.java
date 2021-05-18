/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.collectors.MetricStatus;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.reader_writer_shared.Event;

public class AdmissionControlProcessorTest {

    private static final String DB_URL = "jdbc:sqlite:";
    private static final String TEST_CONTROLLER = "testController";
    private static final String TEST_REJECTION_COUNT = "1";

    private AdmissionControlProcessor admissionControlProcessor;
    private long currentTimestamp;
    private NavigableMap<Long, AdmissionControlSnapshot> snapshotMap;
    Connection connection;

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        connection = DriverManager.getConnection(DB_URL);
        this.currentTimestamp = System.currentTimeMillis();
        this.snapshotMap = new TreeMap<>();
        this.admissionControlProcessor =
                AdmissionControlProcessor.build(currentTimestamp, connection, snapshotMap);
    }

    @Test
    public void testHandleEvent() throws Exception {
        Event testEvent = buildTestAdmissionControlEvent();

        admissionControlProcessor.initializeProcessing(
                currentTimestamp, System.currentTimeMillis());

        assertTrue(admissionControlProcessor.shouldProcessEvent(testEvent));

        admissionControlProcessor.processEvent(testEvent);
        admissionControlProcessor.finalizeProcessing();

        AdmissionControlSnapshot snap = snapshotMap.get(currentTimestamp);
        Result<Record> result = snap.fetchAll();
        assertEquals(1, result.size());
        Assert.assertEquals(
                TEST_CONTROLLER,
                result.get(0).get(AdmissionControlSnapshot.Fields.CONTROLLER_NAME.toString()));
        Assert.assertEquals(
                Long.parseLong(TEST_REJECTION_COUNT),
                result.get(0).get(AdmissionControlSnapshot.Fields.REJECTION_COUNT.toString()));
    }

    private Event buildTestAdmissionControlEvent() {
        long currentTimeMillis = System.currentTimeMillis();
        String admissionControlEvent =
                new AdmissionControlMetrics(
                                TEST_CONTROLLER, 0L, 0L, Long.parseLong(TEST_REJECTION_COUNT))
                        .serialize();
        return new Event(
                PerformanceAnalyzerMetrics.sAdmissionControlMetricsPath,
                admissionControlEvent,
                currentTimeMillis);
    }

    static class AdmissionControlMetrics extends MetricStatus {

        private String controllerName;
        private long currentValue;
        private long thresholdValue;
        private long rejectionCount;

        public AdmissionControlMetrics() {
            super();
        }

        public AdmissionControlMetrics(
                String controllerName,
                long currentValue,
                long thresholdValue,
                long rejectionCount) {
            super();
            this.controllerName = controllerName;
            this.currentValue = currentValue;
            this.thresholdValue = thresholdValue;
            this.rejectionCount = rejectionCount;
        }

        @JsonProperty(AllMetrics.AdmissionControlDimension.Constants.CONTROLLER_NAME)
        public String getControllerName() {
            return controllerName;
        }

        @JsonProperty(AllMetrics.AdmissionControlValue.Constants.CURRENT_VALUE)
        public long getCurrentValue() {
            return currentValue;
        }

        @JsonProperty(AllMetrics.AdmissionControlValue.Constants.THRESHOLD_VALUE)
        public long getThresholdValue() {
            return thresholdValue;
        }

        @JsonProperty(AllMetrics.AdmissionControlValue.Constants.REJECTION_COUNT)
        public long getRejectionCount() {
            return rejectionCount;
        }
    }
}
