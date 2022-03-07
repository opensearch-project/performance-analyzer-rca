/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.reader_writer_shared.Event;

public class EventDispatcher {

    private static final Logger LOG = LogManager.getLogger(EventDispatcher.class);

    private List<EventProcessor> eventProcessors = new ArrayList<>();

    void registerEventProcessor(EventProcessor processor) {
        eventProcessors.add(processor);
    }

    void initializeProcessing(long startTime, long endTime) {
        for (EventProcessor p : eventProcessors) {
            p.initializeProcessing(startTime, endTime);
        }
    }

    void finalizeProcessing() {
        for (EventProcessor p : eventProcessors) {
            p.finalizeProcessing();
        }
    }

    public void processEvent(Event event) {
        boolean eventProcessed = false;
        for (EventProcessor p : eventProcessors) {
            if (p.shouldProcessEvent(event)) {
                p.processEvent(event);
                p.commitBatchIfRequired();
                eventProcessed = true;
                break;
            }
        }

        if (!eventProcessed) {
            LOG.error("Event not processed - {}", event.key);
        }
    }
}
