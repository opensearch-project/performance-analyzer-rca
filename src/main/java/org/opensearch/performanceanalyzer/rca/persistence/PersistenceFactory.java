/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.persistence;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import org.opensearch.performanceanalyzer.rca.exceptions.MalformedConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;

/**
 * The interfaces are called to read data for a resource by the runtime before calling the operate
 * method on the node, The runtime also calls it after the operate completes and returns the value
 * to persist the results. The write of the persistor is also called by the network interfaces to
 * persist what the remote node sends. The persistor, is adaptor based. It takes in which data-store
 * to persist the data in as mentioned in the rca.conf file. The available data-stores can be files
 * on disk in some format, SQl lite or S3 or anything new we want to persist to tomorrow. Users in
 * OSS can write an adaptor to their favorite data store.
 */
public class PersistenceFactory {
    public static Persistable create(RcaConf rcaConf)
            throws MalformedConfig, SQLException, IOException {
        Map<String, String> datastore = rcaConf.getDatastore();
        switch (datastore.get(RcaConsts.DATASTORE_TYPE_KEY).toLowerCase()) {
            case "sqlite":
                return new SQLitePersistor(
                        datastore.get(RcaConsts.DATASTORE_LOC_KEY),
                        datastore.get(RcaConsts.DATASTORE_FILENAME),
                        datastore.get(RcaConsts.DATASTORE_STORAGE_FILE_RETENTION_COUNT),
                        RcaConsts.DB_FILE_ROTATION_TIME_UNIT,
                        RcaConsts.ROTATION_PERIOD);
            default:
                String err = "The datastore value can only be sqlite in any case format";
                throw new MalformedConfig(rcaConf.getConfigFileLoc(), err);
        }
    }
}
