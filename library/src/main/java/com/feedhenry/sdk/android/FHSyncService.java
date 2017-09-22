/**
 * Copyright Red Hat, Inc, and individual contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.feedhenry.sdk.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.feedhenry.sdk.exceptions.DataSetNotFound;
import com.feedhenry.sdk.exceptions.FHNotReadyException;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.network.SyncNetworkCallback;
import com.feedhenry.sdk.storage.Storage;
import com.feedhenry.sdk.sync.*;
import com.feedhenry.sdk.utils.Logger;
import com.feedhenry.sdk.utils.UtilFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static com.feedhenry.sdk.sync.FHSyncUtils.notNullDo;

/**
 * Service that handles synchronization on background.
 */
public class FHSyncService extends IntentService {

    public static final String SERVICE_NAME = "FHSyncService";
    private static final String UTF_8 = "UTF-8";
    private static final String KEY_SYNC_CONFIG = "syncConfig";
    private static final String KEY_DATASET_SYNC_CONFIG = "datasetSyncConfig";
    private static final String KEY_DATASET_QUERY_PARAMS = "datasetQueryParams";
    private static final String KEY_DATASET_METADATA = "datasetMetadata";
    private static final String KEY_MANAGED_DATASETS = "managedDatasets";
    private static final String KEY_CLOUD_URL = "cloudURL";
    private IBinder binder = new FHSyncBinder(this);
    private FHSyncClient syncClient;
    private final static String GLOBAL_CONFIG = "fh_sync_config";

    private FHSyncConfig syncConfig = (new FHSyncConfig.Builder()).build();
    private Storage storage;
    private UtilFactory utilFactory;
    private Logger log;
    private final HashMap<String, ManagedDatasetConfig> managedDatasets = new HashMap<>();
    private final LinkedList<WeakSyncListener> syncListeners = new LinkedList<>();
    private NetworkClient networkClient;
    private String cloudURL;
    private FHSyncListener syncListener = new FHSyncListener() {
        @Override
        public void onSyncStarted(NotificationMessage message) {
            notifyListeners(listener -> listener.onSyncStarted(message));
        }

        @Override
        public void onSyncCompleted(NotificationMessage message) {
            notifyListeners(listener -> listener.onSyncCompleted(message));
        }

        @Override
        public void onUpdateOffline(NotificationMessage message) {
            notifyListeners(listener -> listener.onUpdateOffline(message));
        }

        @Override
        public void onCollisionDetected(NotificationMessage message) {
            notifyListeners(listener -> listener.onCollisionDetected(message));
        }

        @Override
        public void onRemoteUpdateFailed(NotificationMessage message) {
            notifyListeners(listener -> listener.onRemoteUpdateFailed(message));
        }

        @Override
        public void onRemoteUpdateApplied(NotificationMessage message) {
            notifyListeners(listener -> listener.onRemoteUpdateFailed(message));
        }

        @Override
        public void onLocalUpdateApplied(NotificationMessage message) {
            notifyListeners(listener -> listener.onLocalUpdateApplied(message));
        }

        @Override
        public void onDeltaReceived(NotificationMessage message) {
            notifyListeners(listener -> listener.onDeltaReceived(message));
        }

        @Override
        public void onSyncFailed(NotificationMessage message) {
            notifyListeners(listener -> listener.onSyncFailed(message));
        }

        @Override
        public void onClientStorageFailed(NotificationMessage message) {
            notifyListeners(listener -> listener.onClientStorageFailed(message));
        }

        private void notifyListeners(FHSyncUtils.Action1<FHSyncListener> eventFunc) {
            for (Iterator<WeakSyncListener> i = syncListeners.iterator(); i.hasNext(); ) {
                WeakSyncListener syncListener = i.next();
                FHSyncListener wrappedListener = syncListener.getWrapped();
                if (wrappedListener != null) {
                    eventFunc.doAction(wrappedListener);
                } else {
                    log.w(SERVICE_NAME, "not delivering to dead FHSyncListner, don't forget to call removeListener()");
                    i.remove();
                }
            }
        }
    };

    public FHSyncService() {
        super(SERVICE_NAME);
    }

    public FHSyncService(String name) {
        super(name);
    }

    static class FHSyncBinder extends Binder {

        private final FHSyncService service;

        private FHSyncBinder(FHSyncService service) {
            this.service = service;
        }

        public FHSyncService getService() {
            return service;
        }

    }

    private class ManagedDatasetConfig {

        FHSyncConfig syncConfig;
        JSONObject queryParams;
        JSONObject metadata;

        public ManagedDatasetConfig(FHSyncConfig syncConfig, JSONObject queryParams, JSONObject metadata) {
            this.syncConfig = syncConfig;
            this.queryParams = queryParams;
            this.metadata = metadata;
        }
    }

    public UtilFactory getUtilFactory() {
        if (utilFactory == null) {
            utilFactory = new AndroidUtilFactory(this);
        }
        return utilFactory;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log = getUtilFactory().getLogger();
        storage = getUtilFactory().getStorage();
        networkClient = getUtilFactory().getNetworkClient();
        networkClient.registerNetworkListener();
        log.d(SERVICE_NAME, "Service " + toString() + " created.");
        try {
            loadSyncConfig();
        } catch (FileNotFoundException e) {
            log.i(SERVICE_NAME, "Sync config not yet set.");
        } catch (IOException e) {
            log.e(SERVICE_NAME, "Unable to open config.", e);
        }
        syncClient = new FHSyncClient(syncConfig, getUtilFactory());
        syncClient.setListener(syncListener);

        for (Map.Entry<String, ManagedDatasetConfig> entry : managedDatasets.entrySet()) {
            String dataId = entry.getKey();
            ManagedDatasetConfig managedDatasetConfig = entry.getValue();
            syncClient.manage(dataId, managedDatasetConfig.syncConfig, managedDatasetConfig.queryParams, managedDatasetConfig.metadata);
            log.d(SERVICE_NAME, "Restoring management of dataset " + dataId);
        }
        syncClient.stopSync(false);
        log.d(SERVICE_NAME, "Sync client initialized.");
    }

    private void loadSyncConfig() throws IOException {
        byte[] configBody = storage.getContent(GLOBAL_CONFIG);
        try {
            JSONObject configJSON = new JSONObject(new String(configBody, UTF_8));
            JSONObject syncConfigJSON = configJSON.getJSONObject(KEY_SYNC_CONFIG);
            syncConfig = (new FHSyncConfig.Builder()).fromJSON(syncConfigJSON).build();
            managedDatasets.clear();

            JSONObject managedDatasetsJSON = configJSON.getJSONObject(KEY_MANAGED_DATASETS);
            setCloudUrl(configJSON.optString(KEY_CLOUD_URL));
            for (Iterator<String> it = managedDatasetsJSON.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject managedDatasetJSON = managedDatasetsJSON.getJSONObject(key);
                JSONObject datasetSyncConfig = managedDatasetJSON.optJSONObject(KEY_DATASET_SYNC_CONFIG);
                FHSyncConfig fhSyncConfig = null;
                if (datasetSyncConfig != null) {
                    fhSyncConfig = (new FHSyncConfig.Builder()).fromJSON(datasetSyncConfig).build();
                }
                JSONObject queryParams = managedDatasetJSON.optJSONObject(KEY_DATASET_QUERY_PARAMS);
                JSONObject metadata = managedDatasetJSON.optJSONObject(KEY_DATASET_METADATA);
                ManagedDatasetConfig datasetConfig = new ManagedDatasetConfig(fhSyncConfig, queryParams, metadata);
                managedDatasets.put(key, datasetConfig);
                log.d(GLOBAL_CONFIG, "Loaded config for dataset: " + key);
            }
        } catch (JSONException e) {
            log.e(GLOBAL_CONFIG, "Error parsing sync config, using defaults.", e);
            syncConfig = new FHSyncConfig.Builder().build();
        }
    }

    private void saveSyncConfig() throws IOException {
        try {
            JSONObject configJSON = new JSONObject();
            configJSON.put(KEY_SYNC_CONFIG, syncConfig.toJSON());
            JSONObject managedDatasetsJSON = new JSONObject();
            for (Map.Entry<String, ManagedDatasetConfig> entry : managedDatasets.entrySet()) {
                ManagedDatasetConfig datasetConfig = entry.getValue();
                JSONObject managedDatasetJSON = new JSONObject();
                notNullDo(datasetConfig.syncConfig, config -> {
                    try {
                        managedDatasetJSON.put(KEY_DATASET_SYNC_CONFIG, config.toJSON());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
                managedDatasetJSON.put(KEY_DATASET_QUERY_PARAMS, datasetConfig.queryParams);
                managedDatasetJSON.put(KEY_DATASET_METADATA, datasetConfig.metadata);
                managedDatasetsJSON.put(entry.getKey(), managedDatasetJSON);
            }
            configJSON.put(KEY_MANAGED_DATASETS, managedDatasetsJSON);
            configJSON.put(KEY_CLOUD_URL, cloudURL);
            storage.putContent(GLOBAL_CONFIG, configJSON.toString().getBytes(UTF_8));
            log.d(SERVICE_NAME, "Saved sync config persistently.");
        } catch (JSONException e) {
            log.e(SERVICE_NAME, "Unable to create sync config JSON.", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log.d(SERVICE_NAME, "Service " + toString() + " bound.");
        return binder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    public void setConfig(FHSyncConfig config) {
        syncConfig = config;
        log.d(SERVICE_NAME, "Sync config set");
        try {
            saveSyncConfig();
        } catch (IOException e) {
            log.e(SERVICE_NAME, "Unable to save sync config persistently.", e);
        }
    }

    public void setCloudUrl(String url) {
        cloudURL = url;
        networkClient.setCloudURL(url);
    }

    /**
     * Uses the sync client to manage a dataset.
     *
     * @param pDataId      The id of the dataset.
     * @param pConfig      The sync configuration for the dataset. If not specified,
     *                     the sync configuration passed in the initDev method will be used
     * @param pQueryParams Query parameters for the dataset
     *
     * @throws IllegalStateException thrown if FHSyncClient isn't initialised.
     */
    public void manage(String pDataId, FHSyncConfig pConfig, JSONObject pQueryParams) {
        manage(pDataId, pConfig, pQueryParams, new JSONObject());
    }

    /**
     * Uses the sync client to manage a dataset.
     *
     * @param pDataId      The id of the dataset.
     * @param pConfig      The sync configuration for the dataset. If not specified,
     *                     the sync configuration passed in the initDev method will be used
     * @param pQueryParams Query parameters for the dataset
     * @param pMetaData    Meta for the dataset
     *
     * @throws IllegalStateException thrown if FHSyncClient isn't initialised.
     */
    public void manage(String pDataId, FHSyncConfig pConfig, JSONObject pQueryParams, JSONObject pMetaData) {
        log.d(SERVICE_NAME, "managing dataset " + pDataId);
        syncClient.manage(pDataId, pConfig, pQueryParams, pMetaData);
        managedDatasets.put(pDataId, new ManagedDatasetConfig(pConfig, pQueryParams, pMetaData));
        try {
            saveSyncConfig();
        } catch (IOException e) {
            log.e(SERVICE_NAME, "Unable to save sync config persistently.", e);
        }
    }

    /**
     * Causes the sync framework to schedule for immediate execution a sync.
     *
     * @param pDataId The id of the dataset
     */
    public void forceSync(String pDataId) {
        syncClient.forceSync(pDataId);
    }

    /**
     * Lists all the data in the dataset with pDataId.
     *
     * @param pDataId The id of the dataset
     *
     * @return all data records. Each record contains a key "uid" with the id
     * value and a key "data" with the JSON data.
     */
    public JSONObject list(String pDataId) {
        return syncClient.list(pDataId);
    }

    /**
     * Reads a data record with pUID in dataset with pDataId.
     *
     * @param pDataId the id of the dataset
     * @param pUID    the id of the data record
     *
     * @return the data record. Each record contains a key "uid" with the id
     * value and a key "data" with the JSON data.
     */
    public JSONObject read(String pDataId, String pUID) {
        return syncClient.read(pDataId, pUID);
    }

    /**
     * Creates a new data record in dataset with pDataId.
     *
     * @param pDataId the id of the dataset
     * @param pData   the actual data
     *
     * @return the created data record. Each record contains a key "uid" with
     * the id value and a key "data" with the JSON data.
     *
     * @throws DataSetNotFound if the dataId is not known
     */
    public JSONObject create(String pDataId, JSONObject pData) throws DataSetNotFound {
        return syncClient.create(pDataId, pData);
    }

    /**
     * Updates an existing data record in dataset with pDataId.
     *
     * @param pDataId the id of the dataset
     * @param pUID    the id of the data record
     * @param pData   the new content of the data record
     *
     * @return the updated data record. Each record contains a key "uid" with
     * the id value and a key "data" with the JSON data.
     *
     * @throws DataSetNotFound if the dataId is not known
     */
    public JSONObject update(String pDataId, String pUID, JSONObject pData) throws DataSetNotFound {
        return syncClient.update(pDataId, pUID, pData);
    }

    /**
     * Deletes a data record in the dataset with pDataId.
     *
     * @param pDataId the id of the dataset
     * @param pUID    the id of the data record
     *
     * @return the deleted data record. Each record contains a key "uid" with
     * the id value and a key "data" with the JSON data.
     *
     * @throws DataSetNotFound if the dataId is not known
     */
    public JSONObject delete(String pDataId, String pUID) throws DataSetNotFound {
        return syncClient.delete(pDataId, pUID);
    }

    /**
     * Lists sync collisions in dataset with id pDataId.
     *
     * @param pDataId   the id of the dataset
     * @param pCallback the callback function
     *
     * @throws FHNotReadyException if FH is not initialized.
     */
    public void listCollisions(String pDataId, SyncNetworkCallback pCallback) throws FHNotReadyException {
        syncClient.listCollisions(pDataId, pCallback);
    }

    /**
     * Removes a sync collision record in the dataset with id pDataId.
     *
     * @param pDataId        the id of the dataset
     * @param pCollisionHash the hash value of the collision record
     * @param pCallback      the callback function
     *
     * @throws FHNotReadyException thrown if Sync is not initialized.
     */
    public void removeCollision(String pDataId, String pCollisionHash, SyncNetworkCallback pCallback) throws FHNotReadyException {
        syncClient.removeCollision(pDataId, pCollisionHash, pCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        networkClient.unregisterNetworkListener();
        syncClient.destroy();
        log.d(SERVICE_NAME, "Service " + toString() + " destroyed.");
    }

    public void registerListener(FHSyncListener listener) {
        log.d(SERVICE_NAME, "listener " + listener.toString() + " registration");
        syncListeners.add(new WeakSyncListener(listener));
    }

    public void unregisterListener(FHSyncListener listener) {
        for (Iterator<WeakSyncListener> i = syncListeners.iterator(); i.hasNext(); ) {
            WeakSyncListener syncListener = i.next();
            if (syncListener.hasLeaked() || syncListener.getWrapped() == listener) { //removes leaked listeners or listener matching one you want to unregister
                i.remove();
                log.d(SERVICE_NAME, "listener " + listener.toString() + " unregistration");
            }
        }
    }

}
