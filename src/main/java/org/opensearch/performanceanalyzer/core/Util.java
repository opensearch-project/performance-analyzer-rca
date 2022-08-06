/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.core;


import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;

public class Util {
    private static final Logger LOG = LogManager.getLogger(Util.class);

    public static final String PA_BASE_URL = "/_plugins/_performanceanalyzer";
    public static final String LEGACY_OPENDISTRO_PA_BASE_URL = PA_BASE_URL;

    public static final String METRICS_QUERY_URL = PA_BASE_URL + "/metrics";
    public static final String LEGACY_OPENDISTRO_METRICS_QUERY_URL =
            LEGACY_OPENDISTRO_PA_BASE_URL + "/metrics";

    public static final String BATCH_METRICS_URL = PA_BASE_URL + "/batch";
    public static final String LEGACY_OPENDISTRO_BATCH_METRICS_URL =
            LEGACY_OPENDISTRO_PA_BASE_URL + "/batch";

    public static final String RCA_QUERY_URL = PA_BASE_URL + "/rca";
    public static final String LEGACY_OPENDISTRO_RCA_QUERY_URL =
            LEGACY_OPENDISTRO_PA_BASE_URL + "/rca";

    public static final String ACTIONS_QUERY_URL = PA_BASE_URL + "/actions";
    public static final String LEGACY_OPENDISTRO_ACTIONS_QUERY_URL =
            LEGACY_OPENDISTRO_PA_BASE_URL + "/actions";

    public static final String OPENSEARCH_HOME = System.getProperty("opensearch.path.home");
    public static final String PLUGIN_LOCATION =
            OPENSEARCH_HOME
                    + File.separator
                    + "plugins"
                    + File.separator
                    + "opensearch-performance-analyzer"
                    + File.separator;
    public static final String READER_LOCATION =
            OPENSEARCH_HOME + File.separator + "performance-analyzer-rca" + File.separator;
    public static final String DATA_DIR =
            OPENSEARCH_HOME + File.separator + "data" + File.separator;

    public static void invokePrivileged(Runnable runner) {
        AccessController.doPrivileged(
                (PrivilegedAction<Void>)
                        () -> {
                            try {
                                runner.run();
                            } catch (Exception ex) {
                                LOG.debug(
                                        (Supplier<?>)
                                                () ->
                                                        new ParameterizedMessage(
                                                                "Privileged Invocation failed {}",
                                                                ex.toString()),
                                        ex);
                            }
                            return null;
                        });
    }

    public static void invokePrivilegedAndLogError(Runnable runner) {
        AccessController.doPrivileged(
                (PrivilegedAction<Void>)
                        () -> {
                            try {
                                runner.run();
                            } catch (Exception ex) {
                                LOG.error(
                                        (Supplier<?>)
                                                () ->
                                                        new ParameterizedMessage(
                                                                "Privileged Invocation failed {}",
                                                                ex.toString()),
                                        ex);
                            }
                            return null;
                        });
    }
}
