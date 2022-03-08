/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader_writer_shared;

public class Event {
    public String key;
    public String value;
    public long epoch;

    public Event(String key, String value, long epoch) {
        this.key = key;
        this.value = value;
        this.epoch = epoch;
    }

    @Override
    public String toString() {
        return String.format("%s:%d::%s", key, epoch, value);
    }
}
