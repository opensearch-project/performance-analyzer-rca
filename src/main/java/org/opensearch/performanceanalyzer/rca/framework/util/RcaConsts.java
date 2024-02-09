/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.util;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.opensearch.performanceanalyzer.commons.util.Util;

public class RcaConsts {

    public static final String RCA_CONF_CLUSTER_MANAGER_FILENAME = "rca_cluster_manager.conf";
    public static final String RCA_NETWORK_THREAD_NAME_FORMAT = "rca-net-%d";
    public static final int NETWORK_CORE_THREAD_COUNT = 1;
    public static final int NETWORK_MAX_THREAD_COUNT = 1;
    public static final int DEFAULT_PER_NODE_FLOWUNIT_Q_SIZE = 200;
    public static final long RCA_STATE_CHECK_INTERVAL_IN_MS = 5000;
    private static final String RCA_CONF_FILENAME = "rca.conf";
    private static final String RCA_CONF_IDLE_CLUSTER_MANAGER_FILENAME =
            "rca_idle_cluster_manager.conf";
    private static final String THRESHOLDS_DIR_NAME = "thresholds";
    public static final String CONFIG_DIR_PATH =
            Paths.get(Util.READER_LOCATION, "config").toString();
    public static final String RCA_CONF_PATH =
            Paths.get(CONFIG_DIR_PATH, RCA_CONF_FILENAME).toString();
    public static final String RCA_CONF_CLUSTER_MANAGER_PATH =
            Paths.get(CONFIG_DIR_PATH, RCA_CONF_CLUSTER_MANAGER_FILENAME).toString();
    public static final String RCA_CONF_IDLE_CLUSTER_MANAGER_PATH =
            Paths.get(CONFIG_DIR_PATH, RCA_CONF_IDLE_CLUSTER_MANAGER_FILENAME).toString();
    public static final String THRESHOLDS_PATH =
            Paths.get(CONFIG_DIR_PATH, THRESHOLDS_DIR_NAME).toString();

    static final String dir = System.getProperty("user.dir");
    public static final String TEST_CONFIG_PATH =
            Paths.get(dir, "src", "test", "resources", "rca").toString();
    // Paths.get(dir, "build", "resources", "test", "rca").toString();

    // These are some of the constants that will be expected in the rca.conf file.
    public static final String DATASTORE_TYPE_KEY = "type";
    public static final String DATASTORE_LOC_KEY = "location-dir";
    public static final String DATASTORE_FILENAME = "filename";
    public static final String DATASTORE_TIMESTAMP_COL_NAME = "timestamp";
    public static final String DATASTORE_RESOURCE_COL_NAME = "resource";
    public static final String DATASTORE_STATE_COL_NAME = "state";
    public static final String DATASTORE_STORAGE_FILE_RETENTION_COUNT =
            "storage-file-retention-count";

    // The next two lines says that the RCA sqlite files needs to be rotated every hour
    public static final TimeUnit DB_FILE_ROTATION_TIME_UNIT = TimeUnit.HOURS;
    public static final long ROTATION_PERIOD = 1;

    public static final long rcaNannyPollerPeriodicity = 5;
    public static final long rcaConfPollerPeriodicity = 5;
    public static final long nodeRolePollerPeriodicityInSeconds = 60;
    public static final TimeUnit rcaPollerPeriodicityTimeUnit = TimeUnit.SECONDS;

    /** Class defining constants that are mostly used in tag assignment and comparison context. */
    public static class RcaTagConstants {

        public static final String SEPARATOR = ",";

        public static final String TAG_LOCUS = "locus";
        public static final String TAG_AGGREGATE_UPSTREAM = "aggregate-upstream";

        public static final String LOCUS_DATA_NODE = "data-node";
        public static final String LOCUS_CLUSTER_MANAGER_NODE = "cluster_manager-node";
        public static final String LOCUS_DATA_CLUSTER_MANAGER_NODE =
                String.join(
                        RcaTagConstants.SEPARATOR,
                        RcaTagConstants.LOCUS_DATA_NODE,
                        RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
    }
}
