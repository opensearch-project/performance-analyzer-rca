/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.HeatZoneAssigner;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureVector;

public class NodeLevelDimensionalSummaryTest {
    @Test
    public void getShardsInReverseTemperatureOrder() {
        final HeatZoneAssigner.Zone ZONE = HeatZoneAssigner.Zone.HOT;
        final TemperatureDimension DIMENSION = TemperatureDimension.CPU_Utilization;
        final int SHARD_COUNT = 10;

        NodeLevelDimensionalSummary nodeSummary =
                new NodeLevelDimensionalSummary(
                        DIMENSION, new TemperatureVector.NormalizedValue((short) 2), 12.0);

        // The list of shards so obtained is shards ordered in ascending order of temperature
        // along the Dimension = DIMENSION.
        for (ShardProfileSummary shard : getShards(DIMENSION, SHARD_COUNT)) {
            nodeSummary.addShardToZone(shard, ZONE);
        }

        List<ShardProfileSummary> shards =
                nodeSummary.getShardsForZoneInReverseTemperatureOrder(HeatZoneAssigner.Zone.HOT);

        // Although the shards were inserted in the ascending order of temperature,
        // the getShardsForZoneInReverseTemperatureOrder should return them in the
        // descending order of temperature.
        for (int i = 0; i < shards.size(); i++) {
            ShardProfileSummary shard = shards.get(i);
            Assert.assertEquals(
                    SHARD_COUNT - i - 1, shard.getHeatInDimension(DIMENSION).getPOINTS());
        }
    }

    private List<ShardProfileSummary> getShards(TemperatureDimension dimension, int count) {
        List<ShardProfileSummary> shards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ShardProfileSummary shard = new ShardProfileSummary("test-index", i);
            shard.addTemperatureForDimension(
                    dimension, new TemperatureVector.NormalizedValue((short) i));
            shards.add(shard);
        }
        return shards;
    }
}
