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
package com.feedhenry.sdk.sync;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.feedhenry.sdk.exceptions.DataSetNotFound;
import com.feedhenry.sdk.exceptions.FHNotReadyException;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.network.SyncNetworkCallback;
import com.feedhenry.sdk.utils.Logger;
import com.feedhenry.sdk.utils.UtilFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The sync client is part of the FeedHenry data sync framework. It provides a
 * mechanism to manage bi-direction data synchronization. For more details,
 * please check
 * <a href="http://docs.feedhenry.com/v3/guides/sync_service.html">data sync
 * framework docs</a>.
 */
public class FHSyncClient {


    private static final String LOG_TAG = "FHSyncClient";

    private Handler handler;
    private NetworkClient networkClient;

    private Map<String, FHSyncDataset> datasets = new HashMap<>();
    private FHSyncConfig config = new FHSyncConfig();
    private FHSyncListener syncListener = null;

    private FHSyncNotificationHandler notificationHandler;

    private boolean initialized = false;
    private MonitorTask monitorTask = null;
    private UtilFactory utilFactory;
    private Logger log;
    private HandlerThread syncClientThread;
    private HandlerThread monitorThread;

    /**
     * Creates and initializes the sync client and starts sync threads.
     * starts.
     *
     * @param config      The sync configuration
     * @param utilFactory Utility class factory (it provides storage, network access, etc.)
     */
    public FHSyncClient(FHSyncConfig config, UtilFactory utilFactory) {
        this.config = config;
        this.utilFactory = utilFactory;
        this.networkClient = utilFactory.getNetworkClient();
        this.log = utilFactory.getLogger();

        initHandlers();
        initialized = true;

        syncClientThread = new HandlerThread("FHSyncClient");
        syncClientThread.start();
        handler = new Handler(syncClientThread.getLooper());
        if (null == monitorTask) {
            monitorThread = new HandlerThread("FHSyncClient_monitor");
            monitorThread.start();
            Handler handler = new Handler(monitorThread.getLooper());
            monitorTask = new MonitorTask();
            handler.post(monitorTask);
        }
    }

    /**
     * Initializes the notification handlers.
     */
    private void initHandlers() {
        if (null != Looper.myLooper()) {
            notificationHandler = new FHSyncNotificationHandler(this.syncListener);
        } else {
            HandlerThread ht = new HandlerThread("FHSyncClientNotificationHanlder");
            ht.start();
            notificationHandler = new FHSyncNotificationHandler(ht.getLooper(), this.syncListener);
        }
    }

    /**
     * Re-sets the sync listener.
     *
     * @param listener the new sync listener
     */
    public void setListener(FHSyncListener listener) {
        syncListener = listener;
        if (null != notificationHandler) {
            notificationHandler.setSyncListener(syncListener);
        }
    }

    /**
     * Uses the sync client to manage a dataset.
     *
     * @param dataId      The id of the dataset.
     * @param config      The sync configuration for the dataset. If not specified,
     *                     the sync configuration passed in the initDev method will be used
     * @param queryParams Query parameters for the dataset
     *
     * @throws IllegalStateException thrown if FHSyncClient isn't initialised.
     */
    public void manage(String dataId, FHSyncConfig config, JSONObject queryParams) {
        manage(dataId, config, queryParams, new JSONObject());
    }

    /**
     * Uses the sync client to manage a dataset.
     *
     * @param dataId      The id of the dataset.
     * @param config      The sync configuration for the dataset. If not specified,
     *                     the sync configuration passed in the initDev method will be used
     * @param queryParams Query parameters for the dataset
     * @param metaData    Meta for the dataset
     *
     * @throws IllegalStateException thrown if FHSyncClient isn't initialised.
     */
    public void manage(String dataId, FHSyncConfig config, JSONObject queryParams, JSONObject metaData) {
        if (!initialized) {
            throw new IllegalStateException("FHSyncClient isn't initialised. Have you called the initDev function?");
        }
        FHSyncDataset dataset = datasets.get(dataId);
        FHSyncConfig syncConfig = this.config;
        if (null != config) {
            syncConfig = config;
        }
        if (null != dataset) {
            dataset.setNotificationHandler(notificationHandler);
        } else {
            dataset = new FHSyncDataset(notificationHandler, dataId, syncConfig, queryParams, metaData, utilFactory);
            datasets.put(dataId, dataset);
            dataset.setSyncRunning(false);
            dataset.setInitialised(true);
        }

        dataset.setSyncConfig(syncConfig);
        dataset.setSyncPending(true);
        try {
            dataset.writeToStorage();
        } catch (JSONException e) {
            log.e(LOG_TAG, "Dataset JSON storage failed.", e);
        }
    }

    /**
     * Causes the sync framework to schedule for immediate execution a sync.
     *
     * @param dataId The id of the dataset
     */
    public void forceSync(String dataId) {
        FHSyncDataset dataset = datasets.get(dataId);

        if (null != dataset) {
            dataset.setSyncPending(true);
        }
    }

    /**
     * Lists all the data in the dataset with dataId.
     *
     * @param dataId The id of the dataset
     *
     * @return all data records. Each record contains a key "uid" with the id
     * value and a key "data" with the JSON data.
     */
    public JSONObject list(String dataId) {
        FHSyncDataset dataset = datasets.get(dataId);
        JSONObject data = null;
        if (null != dataset) {
            data = dataset.listData();
        }
        return data;
    }

    /**
     * Reads a data record with uid in dataset with dataId.
     *
     * @param dataId the id of the dataset
     * @param uid    the id of the data record
     *
     * @return the data record. Each record contains a key "uid" with the id
     * value and a key "data" with the JSON data.
     */
    public JSONObject read(String dataId, String uid) {
        FHSyncDataset dataset = datasets.get(dataId);
        JSONObject data = null;
        if (null != dataset) {
            data = dataset.readData(uid);
        }
        return data;
    }

    /**
     * Creates a new data record in dataset with dataId.
     *
     * @param dataId the id of the dataset
     * @param data   the actual data
     *
     * @return the created data record. Each record contains a key "uid" with
     * the id value and a key "data" with the JSON data.
     *
     * @throws DataSetNotFound if the dataId is not known
     */
    public JSONObject create(String dataId, JSONObject data) throws DataSetNotFound {
        FHSyncDataset dataset = datasets.get(dataId);
        if (null != dataset) {
            return dataset.createData(data);
        } else {
            throw new DataSetNotFound("Unknown dataId : " + dataId);
        }
    }

    /**
     * Updates an existing data record in dataset with dataId.
     *
     * @param dataId the id of the dataset
     * @param uid    the id of the data record
     * @param data   the new content of the data record
     *
     * @return the updated data record. Each record contains a key "uid" with
     * the id value and a key "data" with the JSON data.
     *
     * @throws DataSetNotFound if the dataId is not known
     */
    public JSONObject update(String dataId, String uid, JSONObject data) throws DataSetNotFound {
        FHSyncDataset dataset = datasets.get(dataId);
        if (null != dataset) {
            return dataset.updateData(uid, data);
        } else {
            throw new DataSetNotFound("Unknown dataId : " + dataId);
        }
    }

    /**
     * Deletes a data record in the dataset with dataId.
     *
     * @param dataId the id of the dataset
     * @param uid    the id of the data record
     *
     * @return the deleted data record. Each record contains a key "uid" with
     * the id value and a key "data" with the JSON data.
     *
     * @throws DataSetNotFound if the dataId is not known
     */
    public JSONObject delete(String dataId, String uid) throws DataSetNotFound {
        FHSyncDataset dataset = datasets.get(dataId);
        if (null != dataset) {
            return dataset.deleteData(uid);
        } else {
            throw new DataSetNotFound("Unknown dataId : " + dataId);
        }
    }

    /**
     * Lists sync collisions in dataset with id dataId.
     *
     * @param dataId   the id of the dataset
     * @param callback the callback function
     *
     * @throws FHNotReadyException if FH is not initialized.
     */
    public void listCollisions(String dataId, SyncNetworkCallback callback) throws FHNotReadyException {
        JSONObject params = new JSONObject();
        try {
            params.put("fn", "listCollisions");
            networkClient.performRequest(dataId, params, callback);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes a sync collision record in the dataset with id dataId.
     *
     * @param dataId        the id of the dataset
     * @param collisionHash the hash value of the collision record
     * @param callback      the callback function
     *
     * @throws FHNotReadyException thrown if Sync is not initialized.
     */
    public void removeCollision(String dataId, String collisionHash, SyncNetworkCallback callback) throws FHNotReadyException {
        JSONObject params = new JSONObject();
        try {
            params.put("fn", "removeCollision");
            params.put("hash", collisionHash);
            networkClient.performRequest(dataId, params, callback);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * This method will begin synchronization. It should be called in the
     * {@link Activity#onResume()} block.
     *
     * @param listener the listener to.  If null the current listener will be
     *                 used.
     */
    public void resumeSync(FHSyncListener listener) {

        if (listener != null) {
            this.syncListener = listener;
        }

        stopSync(false);

    }

    /**
     * This starts/stops sync progress on all datasets.
     *
     * @param shouldStopSync true=stop, false=start
     */
    public void stopSync(boolean shouldStopSync) {
        for (FHSyncDataset dataSet : datasets.values()) {
            dataSet.stopSync(shouldStopSync);
        }
    }

    /**
     * This method will pause synchronization. It should be called in the
     * {@link Activity#onPause()} block.
     */
    public void pauseSync() {

        stopSync(true);

        this.syncListener = null;
    }

    /**
     * Stops the sync process for dataset with id dataId.
     *
     * @param dataId the id of the dataset
     */
    public void stop(String dataId) {
        FHSyncDataset dataset = datasets.get(dataId);
        if (null != dataset) {
            dataset.stopSync(true);
        }
    }

    /**
     * Stops all sync processes for all the datasets managed by the sync client.
     */
    public void destroy() {
        if (initialized) {
            if (null != monitorTask) {
                monitorTask.stopRunning();
            }
            if (syncClientThread != null) {
                syncClientThread.quit();
            }
            for (String key : datasets.keySet()) {
                stop(key);
            }
            syncListener = null;
            notificationHandler = null;
            datasets = new HashMap<>();
            initialized = false;
        }
    }

    private class MonitorTask implements Runnable {

        private boolean mKeepRunning = true;

        public void stopRunning() {
            mKeepRunning = false;
            log.d(LOG_TAG, "interrupting MonitorTask");
            monitorThread.quit();
        }

        private void checkDatasets() {
            if (null != datasets) {
                for (Map.Entry<String, FHSyncDataset> entry : datasets.entrySet()) {
                    final FHSyncDataset dataset = entry.getValue();
                    boolean syncRunning = dataset.isSyncRunning();
                    if (!syncRunning && !dataset.isStopSync()) {
                        // sync isn't running for dataId at the moment, check if needs to start it
                        Date lastSyncStart = dataset.getSyncStart();
                        Date lastSyncEnd = dataset.getSyncEnd();
                        if (null == lastSyncStart) {
                            dataset.setSyncPending(true);
                        } else if (null != lastSyncEnd) {
                            long interval = new Date().getTime() - lastSyncEnd.getTime();
                            if (interval > dataset.getSyncConfig().getSyncFrequency() * 1000) {
                                log.d(LOG_TAG, dataset.getDatasetId() + " Should start sync!!");
                                dataset.setSyncPending(true);
                            }
                        }

                        if (dataset.isSyncPending()) {
                            handler.post(dataset::startSyncLoop);
                        }
                    }
                }
            }
        }

        @Override
        public void run() {
            while (mKeepRunning && !Thread.currentThread().isInterrupted()) {
                checkDatasets();
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.e(LOG_TAG, "MonitorTask thread is interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
