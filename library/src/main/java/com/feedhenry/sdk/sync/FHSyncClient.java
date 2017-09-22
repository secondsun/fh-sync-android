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

    private static FHSyncClient mInstance;

    protected static final String LOG_TAG = "FHSyncClient";

    private Handler mHandler;
    private NetworkClient networkClient;

    private Map<String, FHSyncDataset> mDataSets = new HashMap<String, FHSyncDataset>();
    private FHSyncConfig mConfig = new FHSyncConfig();
    private FHSyncListener mSyncListener = null;

    private FHSyncNotificationHandler mNotificationHandler;

    private boolean mInitialised = false;
    private MonitorTask mMonitorTask = null;
    private UtilFactory utilFactory;
    private Logger log;
    private HandlerThread fhSyncClientThread;
    private HandlerThread monitorThread;

    /**
     * Gets the singleton instance of the sync client.
     *
     * @return the sync client instance
     *
     * @deprecated
     */
    public static FHSyncClient getInstance() {
        if (null == mInstance) {
            mInstance = new FHSyncClient();
        }
        return mInstance;
    }

    /**
     * Creates synchronization client.
     */
    public FHSyncClient() {

    }

    /**
     * Initializes the sync client and starts sync threads. Should be called every time an app/activity
     * starts.
     *
     * @param config      The sync configuration
     * @param utilFactory Utility class factory (it provides storage, network access, etc.)
     */
    public void init(FHSyncConfig config, UtilFactory utilFactory) {
        init(config, utilFactory, true);
    }

    /**
     * Initializes the sync client. Should be called every time an app/activity
     * starts.
     *
     * @param config        The sync configuration
     * @param utilFactory   Utility class factory (it provides storage, network access, etc.)
     * @param createThreads Creates background sync threads automatically
     */
    public void init(FHSyncConfig config, UtilFactory utilFactory, boolean createThreads) {
        mConfig = config;
        this.utilFactory = utilFactory;
        this.networkClient = utilFactory.getNetworkClient();
        this.log = utilFactory.getLogger();

        initHandlers();
        mInitialised = true;
        if (createThreads) {
            fhSyncClientThread = new HandlerThread("FHSyncClient");
            fhSyncClientThread.start();
            mHandler = new Handler(fhSyncClientThread.getLooper());
            if (null == mMonitorTask) {
                monitorThread = new HandlerThread("monitor task");
                monitorThread.start();
                Handler handler = new Handler(monitorThread.getLooper());
                mMonitorTask = new MonitorTask();
                handler.post(mMonitorTask);
            }
        }
    }

    /**
     * Initializes the notification handlers.
     */
    private void initHandlers() {
        if (null != Looper.myLooper()) {
            mNotificationHandler = new FHSyncNotificationHandler(this.mSyncListener);
        } else {
            HandlerThread ht = new HandlerThread("FHSyncClientNotificationHanlder");
            ht.start();
            mNotificationHandler = new FHSyncNotificationHandler(ht.getLooper(), this.mSyncListener);
        }
    }

    /**
     * Re-sets the sync listener.
     *
     * @param pListener the new sync listener
     */
    public void setListener(FHSyncListener pListener) {
        mSyncListener = pListener;
        if (null != mNotificationHandler) {
            mNotificationHandler.setSyncListener(mSyncListener);
        }
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
        if (!mInitialised) {
            throw new IllegalStateException("FHSyncClient isn't initialised. Have you called the initDev function?");
        }
        FHSyncDataset dataset = mDataSets.get(pDataId);
        FHSyncConfig syncConfig = mConfig;
        if (null != pConfig) {
            syncConfig = pConfig;
        }
        if (null != dataset) {
            dataset.setNotificationHandler(mNotificationHandler);
        } else {
            dataset = new FHSyncDataset(mNotificationHandler, pDataId, syncConfig, pQueryParams, pMetaData, utilFactory);
            mDataSets.put(pDataId, dataset);
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
     * @param pDataId The id of the dataset
     */
    public void forceSync(String pDataId) {
        FHSyncDataset dataset = mDataSets.get(pDataId);

        if (null != dataset) {
            dataset.setSyncPending(true);
        }
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
        FHSyncDataset dataset = mDataSets.get(pDataId);
        JSONObject data = null;
        if (null != dataset) {
            data = dataset.listData();
        }
        return data;
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
        FHSyncDataset dataset = mDataSets.get(pDataId);
        JSONObject data = null;
        if (null != dataset) {
            data = dataset.readData(pUID);
        }
        return data;
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
        FHSyncDataset dataset = mDataSets.get(pDataId);
        if (null != dataset) {
            return dataset.createData(pData);
        } else {
            throw new DataSetNotFound("Unknown dataId : " + pDataId);
        }
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
        FHSyncDataset dataset = mDataSets.get(pDataId);
        if (null != dataset) {
            return dataset.updateData(pUID, pData);
        } else {
            throw new DataSetNotFound("Unknown dataId : " + pDataId);
        }
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
        FHSyncDataset dataset = mDataSets.get(pDataId);
        if (null != dataset) {
            return dataset.deleteData(pUID);
        } else {
            throw new DataSetNotFound("Unknown dataId : " + pDataId);
        }
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
        JSONObject params = new JSONObject();
        try {
            params.put("fn", "listCollisions");
            networkClient.performRequest(pDataId, params, pCallback);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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
        JSONObject params = new JSONObject();
        try {
            params.put("fn", "removeCollision");
            params.put("hash", pCollisionHash);
            networkClient.performRequest(pDataId, params, pCallback);
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
            this.mSyncListener = listener;
        }

        stopSync(false);

    }

    /**
     * This starts/stops sync progress on all datasets.
     *
     * @param shouldStopSync true=stop, false=start
     */
    public void stopSync(boolean shouldStopSync) {
        for (FHSyncDataset dataSet : mDataSets.values()) {
            dataSet.stopSync(shouldStopSync);
        }
    }

    /**
     * This method will pause synchronization. It should be called in the
     * {@link Activity#onPause()} block.
     */
    public void pauseSync() {

        stopSync(true);

        this.mSyncListener = null;
    }

    /**
     * Stops the sync process for dataset with id pDataId.
     *
     * @param pDataId the id of the dataset
     */
    public void stop(String pDataId) {
        FHSyncDataset dataset = mDataSets.get(pDataId);
        if (null != dataset) {
            dataset.stopSync(true);
        }
    }

    /**
     * Stops all sync processes for all the datasets managed by the sync client.
     */
    public void destroy() {
        if (mInitialised) {
            if (null != mMonitorTask) {
                mMonitorTask.stopRunning();
            }
            if (fhSyncClientThread != null) {
                fhSyncClientThread.quit();
            }
            for (String key : mDataSets.keySet()) {
                stop(key);
            }
            mSyncListener = null;
            mNotificationHandler = null;
            mDataSets = new HashMap<>();
            mInitialised = false;
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
            if (null != mDataSets) {
                for (Map.Entry<String, FHSyncDataset> entry : mDataSets.entrySet()) {
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
                            mHandler.post(dataset::startSyncLoop);
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
