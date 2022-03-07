/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.Condition;
import org.jooq.Field;
import org.junit.Test;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;

public class MetricPropertiesConfigTests extends AbstractReaderTests {

    public MetricPropertiesConfigTests() throws SQLException, ClassNotFoundException {
        super();
    }

    /** Test table names used for metadata table in disk db are unique */
    @Test
    public void testUniqueTableNames() {
        Set<String> seen = new HashSet<>();
        for (Map.Entry<AllMetrics.MetricName, MetricProperties> entry :
                MetricPropertiesConfig.getInstance().getMetricName2Property().entrySet()) {
            MetricProperties property = entry.getValue();
            List<String> metadataTableNames = property.getMetadataTableNames();
            for (String name : metadataTableNames) {
                assertTrue(
                        String.format(
                                "Metric %s has duplicate metadata table name %s",
                                entry.getKey(), name),
                        !seen.contains(name));
                seen.add(name);
            }
        }
    }

    @Test
    public void testConsistentAcrossMaps() {
        for (Map.Entry<AllMetrics.MetricName, MetricProperties> entry :
                MetricPropertiesConfig.getInstance().getMetricName2Property().entrySet()) {
            MetricProperties property = entry.getValue();

            List<String> metadataTableNames = property.getMetadataTableNames();
            Map<String, List<Field<String>>> groupByFields = property.getTableGroupByFieldsMap();
            Map<String, List<Field<?>>> selectFields = property.getTableSelectMap();
            Map<String, Condition> whereClauses = property.getTableWhereClauseMap();

            assertEquals(metadataTableNames.size(), groupByFields.size());
            assertEquals(metadataTableNames.size(), selectFields.size());
            assertEquals(metadataTableNames.size(), whereClauses.size());

            for (String tableName : metadataTableNames) {
                assertTrue(groupByFields.containsKey(tableName));
                assertTrue(selectFields.containsKey(tableName));
                assertTrue(whereClauses.containsKey(tableName));

                List<Field<String>> currGroupByFields = groupByFields.get(tableName);
                List<Field<?>> currSelectFields = selectFields.get(tableName);
                for (Field<String> field : currGroupByFields) {
                    assertTrue(currSelectFields.contains(field));
                }
            }
        }
    }

    /** Test if we have configuration for each MetricName */
    @Test
    public void testMetricNameConsistent() {
        for (AllMetrics.MetricName name : AllMetrics.MetricName.values()) {
            assertTrue(
                    String.format("Missing %s", name),
                    MetricPropertiesConfig.getInstance()
                            .getMetricName2Property()
                            .containsKey(name));
        }
    }
}
