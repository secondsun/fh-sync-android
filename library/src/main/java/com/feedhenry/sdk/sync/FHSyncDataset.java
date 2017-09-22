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

import android.os.Message;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.network.SyncNetworkCallback;
import com.feedhenry.sdk.network.SyncNetworkResponse;
import com.feedhenry.sdk.storage.Storage;
import com.feedhenry.sdk.utils.Logger;
import com.feedhenry.sdk.utils.UtilFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class FHSyncDataset {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private final Storage storage;
    private final NetworkClient networkClient;
    private boolean syncRunning;
    private boolean initialised;
    private final String datasetId;
    private Date syncStart;
    private Date syncEnd;
    private boolean syncPending;
    private FHSyncConfig syncConfig = new FHSyncConfig();
    private final ConcurrentMap<String, FHSyncPendingRecord> pendingRecords = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> uidMappings = new ConcurrentHashMap<>();
    private ConcurrentMap<String, FHSyncDataRecord> dataRecords = new ConcurrentHashMap<>();

    private JSONObject queryParams = new JSONObject();
    private JSONObject metaData = new JSONObject();
    private JSONObject customMetaData = new JSONObject();
    private String hashvalue;
    private JSONArray acknowledgements = new JSONArray();
    private boolean stopSync;
    private Logger log;

    //    private Context mContext;
    private FHSyncNotificationHandler notificationHandler;

    private static final String KEY_DATE_SET_ID = "dataSetId";
    private static final String KEY_SYNC_LOOP_START = "syncLoopStart";
    private static final String KEY_SYNC_LOOP_END = "syncLoopEnd";
    private static final String KEY_SYNC_CONFIG = "syncConfig";
    private static final String KEY_PENDING_RECORDS = "pendingDataRecords";
    private static final String KEY_DATA_RECORDS = "dataRecords";
    private static final String KEY_HASHVALUE = "hashValue";
    private static final String KEY_ACKNOWLEDGEMENTS = "acknowledgements";
    private static final String KEY_QUERY_PARAMS = "queryParams";
    private static final String KEY_METADATA = "metaData";

    private static final String LOG_TAG = "FHSyncDataset";

    FHSyncDataset(FHSyncNotificationHandler handler, String datasetId, FHSyncConfig config, JSONObject queryParams, JSONObject metaData, UtilFactory utilFactory) {
        storage = utilFactory.getStorage();
        networkClient = utilFactory.getNetworkClient();
        log = utilFactory.getLogger();
        notificationHandler = handler;
        this.datasetId = datasetId;
        syncConfig = config;
        this.queryParams = queryParams;
        customMetaData = metaData;
        readFromStorage();
    }

    public JSONObject getJSON() throws JSONException {
        JSONObject ret = new JSONObject();
        if (hashvalue != null) {
            ret.put(KEY_HASHVALUE, hashvalue);
        }
        ret.put(KEY_DATE_SET_ID, datasetId);
        ret.put(KEY_SYNC_CONFIG, syncConfig.toJSON());
        JSONObject pendingJson = new JSONObject();
        for (String key : pendingRecords.keySet()) {
            pendingJson.put(key, pendingRecords.get(key).getJSON());
        }
        ret.put(KEY_PENDING_RECORDS, pendingJson);
        JSONObject dataJson = new JSONObject();
        for (String dkey : dataRecords.keySet()) {
            dataJson.put(dkey, dataRecords.get(dkey).getJSON());
        }
        ret.put(KEY_DATA_RECORDS, dataJson);
        if (this.syncStart != null) {
            ret.put(KEY_SYNC_LOOP_START, this.syncStart.getTime());
        }
        if (this.syncEnd != null) {
            ret.put(KEY_SYNC_LOOP_END, this.syncEnd.getTime());
        }
        ret.put(KEY_ACKNOWLEDGEMENTS, acknowledgements);
        ret.put(KEY_QUERY_PARAMS, queryParams);
        ret.put(KEY_METADATA, metaData);
        return ret;
    }

    public JSONObject listData() {
        JSONObject ret = new JSONObject();
        try {
            for (String key : this.dataRecords.keySet()) {
                FHSyncDataRecord dataRecord = this.dataRecords.get(key);
                JSONObject dataJson = new JSONObject();
                // return a copy of the data so that any changes made to the data will not affect the original data
                dataJson.put("data", new JSONObject(dataRecord.getData().toString()));
                dataJson.put("uid", key);
                ret.put(key, dataJson);
            }
        } catch (JSONException e) {
            log.e(LOG_TAG, "listData(): Unable to create JSON.", e);
        }
        return ret;
    }

    public JSONObject readData(String uid) {
        FHSyncDataRecord dataRecord = dataRecords.get(uid);
        JSONObject ret = new JSONObject();
        if (dataRecord != null) {
            try {
                // return a copy of the data so that any changes made to the data will not affect the original data
                ret.put("data", new JSONObject(dataRecord.getData().toString()));
                ret.put("uid", uid);
            } catch (JSONException e) {
                log.e(LOG_TAG, "readData(): Unable to create JSON.", e);
            }
            return ret;
        } else {
            return null;
        }
    }

    public JSONObject createData(JSONObject data) {
        JSONObject ret = new JSONObject();
        try {
            FHSyncPendingRecord pendingRecord = addPendingObject(null, data, "create");
            FHSyncDataRecord dataRecord = dataRecords.get(pendingRecord.getUid());

            if (dataRecord != null) {
                ret.put("data", new JSONObject(dataRecord.getData().toString()));
                ret.put("uid", pendingRecord.getUid());
            }
        } catch (JSONException e) {
            log.e(LOG_TAG, "createData(): Unable to create JSON.", e);
        }
        return ret;
    }

    public JSONObject updateData(String uid, JSONObject data) {
        JSONObject ret = new JSONObject();
        try {
            addPendingObject(uid, data, "update");
            FHSyncDataRecord dataRecord = dataRecords.get(uid);
            if (dataRecord != null) {
                ret.put("data", new JSONObject(dataRecord.getData().toString()));
                ret.put("uid", uid);
            }
        } catch (JSONException e) {
            log.e(LOG_TAG, "updateData(): Unable to create JSON ", e);
        }
        return ret;
    }

    public JSONObject deleteData(String uid) {
        JSONObject ret = new JSONObject();
        try {
            FHSyncPendingRecord pendingRecord = addPendingObject(uid, null, "delete");
            FHSyncDataRecord deleted = pendingRecord.getPreData();
            if (deleted != null) {
                ret.put("data", new JSONObject(deleted.getData().toString()));
                ret.put("uid", uid);
            }
        } catch (JSONException e) {
            log.e(LOG_TAG, "deleteData(): Unable to create JSON ", e);
        }
        return ret;
    }

    public void startSyncLoop() {
        syncPending = false;
        syncRunning = true;
        syncStart = new Date();
        doNotify(null, NotificationMessage.SYNC_STARTED_CODE, null);
        if (!networkClient.isOnline()) {
            syncCompleteWithCode("offline");
        } else {
            try {
                JSONObject syncLoopParams = new JSONObject();
                syncLoopParams.put("fn", "sync");
                syncLoopParams.put("dataset_id", datasetId);
                syncLoopParams.put("meta_data", customMetaData);
                syncLoopParams.put("query_params", queryParams);
                if (hashvalue != null) {
                    syncLoopParams.put("dataset_hash", hashvalue);
                }
                syncLoopParams.put("acknowledgements", acknowledgements);
                JSONArray pendings = new JSONArray();
                for (String key : pendingRecords.keySet()) {
                    FHSyncPendingRecord pendingRecord = pendingRecords.get(key);
                    if (!pendingRecord.isInFlight() && !pendingRecord.isCrashed() && !pendingRecord.isDelayed()) {
                        pendingRecord.setInFlight(true);
                        pendingRecord.setInFlightDate(new Date());
                        JSONObject pendingJSON = pendingRecord.getJSON();
                        if ("create".equals(pendingRecord.getAction())) {
                            pendingJSON.put("hash", pendingRecord.getUid());
                        } else {
                            pendingJSON.put("hash", pendingRecord.getHashValue());
                        }
                        pendings.put(pendingJSON);
                    }
                }

                syncLoopParams.put("pending", pendings);
                log.d(LOG_TAG, "Starting sync loop -global hash = " + hashvalue + " :: params = " + syncLoopParams);

                try {
                    networkClient.performRequest(datasetId, syncLoopParams, new SyncNetworkCallback() {

                        @Override
                        public void success(SyncNetworkResponse response) {
                            try {
                                JSONObject responseData = response.getJson();
                                syncRequestSuccess(responseData);
                            } catch (JSONException e) {
                                log.e(LOG_TAG, "syncRequestSuccess(): Unable to create JSON ", e);
                            }
                        }

                        @Override
                        public void fail(SyncNetworkResponse response) {
                            /*
                            The AJAX call failed to complete successfully, so the state of the current pending updates
                            is unknown. Mark them as "crashed". The next time a syncLoop completes successfully, we
                            will review the crashed records to see if we can determine their current state.
                            */
                            markInFlightAsCrashed();
                            log.e(LOG_TAG, "syncLoop failed : msg = " + response.getErrorMessage(), response.getError());
                            doNotify(null, NotificationMessage.SYNC_FAILED_CODE, response.getRawResponse());
                            syncCompleteWithCode(response.getRawResponse());
                        }
                    });
                } catch (Exception e) {
                    log.e(LOG_TAG, "Error performing sync", e);
                    doNotify(null, NotificationMessage.SYNC_FAILED_CODE, e.getMessage());
                    syncCompleteWithCode(e.getMessage());
                }
            } catch (JSONException e) {
                log.e(LOG_TAG, "startSyncLoop(): Unable to create JSON ", e);
            }
        }
    }

    private void syncRequestSuccess(JSONObject data) throws JSONException {
        // Check to see if any previously crashed inflight records can now be resolved
        updateCrashedInFlightFromNewData(data);
        updateDelayedFromNewData(data);
        updateMetaFromNewData(data);
        if (data.has("updates")) {
            JSONArray ack = new JSONArray();
            JSONObject updates = data.getJSONObject("updates");
            JSONObject applied = updates.optJSONObject("applied");
            checkUidChanges(applied);
            processUpdates(applied, NotificationMessage.REMOTE_UPDATE_APPLIED_CODE, ack);
            processUpdates(updates.optJSONObject("failed"), NotificationMessage.REMOTE_UPDATE_FAILED_CODE, ack);
            processUpdates(updates.optJSONObject("collisions"), NotificationMessage.COLLISION_DETECTED_CODE, ack);
            acknowledgements = ack;
        }

        if (data.has("hash") && !data.getString("hash").equals(hashvalue)) {
            String remoteHash = data.getString("hash");
            log.d(LOG_TAG, "Local dataset stale - syncing records :: local hash= " + hashvalue + " - remoteHash =" + remoteHash);
            // Different hash value returned - Sync individual records
            syncRecords();
        } else {
            log.i(LOG_TAG, "Local dataset up to date");
        }

        syncCompleteWithCode("online");

    }

    private void syncRecords() {
        try {
            JSONObject clientRecords = new JSONObject();
            for (Map.Entry<String, FHSyncDataRecord> entry : dataRecords.entrySet()) {
                clientRecords.put(entry.getKey(), entry.getValue().getHashValue());
            }

            JSONObject syncRecsParams = new JSONObject();
            syncRecsParams.put("fn", "syncRecords");
            syncRecsParams.put("dataset_id", datasetId);
            syncRecsParams.put("query_params", queryParams);
            syncRecsParams.put("meta_data", customMetaData);
            syncRecsParams.put("clientRecs", clientRecords);

            log.d(LOG_TAG, "syncRecParams :: " + syncRecsParams);

            try {
                networkClient.performRequest(datasetId, syncRecsParams, new SyncNetworkCallback() {

                    @Override
                    public void success(SyncNetworkResponse response) {
                        try {
                            syncRecordsSuccess(response.getJson());
                        } catch (JSONException e) {
                            log.e(LOG_TAG, "Response JSON serialization failed.", e);
                            fail(response);
                        }
                    }

                    @Override
                    public void fail(SyncNetworkResponse response) {
                        log.e(LOG_TAG, "syncRecords failed: " + response.getRawResponse(), response.getError());
                        doNotify(null, NotificationMessage.SYNC_FAILED_CODE, response.getRawResponse());
                        syncCompleteWithCode(response.getRawResponse());
                    }
                });
            } catch (Exception e) {
                log.e(LOG_TAG, "error when running syncRecords", e);
                doNotify(null, NotificationMessage.SYNC_FAILED_CODE, e.getMessage());
                syncCompleteWithCode(e.getMessage());
            }
        } catch (JSONException e) {
            log.e(LOG_TAG, "syncRecords(): Unable to create JSON ", e);
        }
    }

    private void syncRecordsSuccess(JSONObject data) throws JSONException {
        applyPendingChangesToRecords(data);
        handleCreated(data);
        handleUpdated(data);
        handleDeleted(data);

        if (data.has("hash")) {
            hashvalue = data.getString("hash");
        }

        syncCompleteWithCode("online");
    }

    private void handleDeleted(JSONObject data) {
        JSONObject deleted = data.optJSONObject("delete");
        if (deleted != null) {
            for (Iterator<String> it = deleted.keys(); it.hasNext(); ) {
                String key = it.next();
                dataRecords.remove(key);
                doNotify(key, NotificationMessage.DELTA_RECEIVED_CODE, "delete");
            }
        }
    }

    private void handleUpdated(JSONObject data) throws JSONException {
        JSONObject dataUpdated = data.optJSONObject("update");
        if (dataUpdated != null) {
            for (Iterator<String> it = dataUpdated.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject obj = dataUpdated.getJSONObject(key);
                FHSyncDataRecord rec = dataRecords.get(key);
                if (rec != null) {
                    rec.setData(obj.getJSONObject("data"));
                    rec.setHashValue(obj.getString("hash"));
                    dataRecords.put(key, rec);
                    doNotify(key, NotificationMessage.DELTA_RECEIVED_CODE, "update");
                }

            }
        }
    }

    private void handleCreated(JSONObject data) throws JSONException {
        JSONObject created = data.optJSONObject("create");
        if (created != null) {
            for (Iterator<String> it = created.keys(); it.hasNext(); ) {
                String key = it.next();

                JSONObject obj = created.getJSONObject(key);
                FHSyncDataRecord record = new FHSyncDataRecord(obj.getJSONObject("data"));
                record.setHashValue(obj.getString("hash"));
                dataRecords.put(key, record);
                doNotify(key, NotificationMessage.DELTA_RECEIVED_CODE, "create");

            }
        }
    }

    private void processUpdates(JSONObject updates, int pnotification, JSONArray ack) throws JSONException {
        if (updates != null) {
            for (Iterator<String> it = updates.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject up = updates.getJSONObject(key);
                ack.put(up);
                FHSyncPendingRecord pendingRec = pendingRecords.get(key);
                if (pendingRec != null && pendingRec.isInFlight() && !pendingRec.isCrashed()) {
                    pendingRecords.remove(key);
                    doNotify(up.getString("uid"), pnotification, up.toString());
                }
            }
        }
    }

    private void updateCrashedInFlightFromNewData(JSONObject remoteData) throws JSONException {

        JSONObject resolvedCrashed = new JSONObject();
        List<String> keysToRemove = new ArrayList<String>();

        for (Map.Entry<String, FHSyncPendingRecord> pendingRecordEntry : pendingRecords.entrySet()) {
            FHSyncPendingRecord pendingRecord = pendingRecordEntry.getValue();
            String pendingHash = pendingRecordEntry.getKey();
            if (pendingRecord.isInFlight() && pendingRecord.isCrashed()) {
                log.d(LOG_TAG, String.format("updateCrashedInFlightFromNewData - " + "Found crashed inFlight pending record uid= %s :: hash %s", pendingRecord.getUid(), pendingRecord.getHashValue()));
                if (remoteData != null && remoteData.has("updates") && remoteData.getJSONObject("updates").has("hashes")) {
                    JSONObject hashes = remoteData.getJSONObject("updates").getJSONObject("hashes");
                    JSONObject crashedUpdate = hashes.optJSONObject(pendingHash);
                    if (crashedUpdate != null) {
                        resolvedCrashed.put(crashedUpdate.getString("uid"), crashedUpdate);
                        log.d(LOG_TAG, "updateCrashedInFlightFromNewData - Resolving status for crashed inflight pending record " + crashedUpdate.toString());
                        String crashedType = crashedUpdate.optString("type");
                        String crashedAction = crashedUpdate.optString("action");

                        if (crashedType != null && crashedType.equals("failed")) {
                            // Crashed updated failed - revert local dataset
                            if (crashedAction != null && crashedAction.equals("create")) {
                                log.d(LOG_TAG, "updateCrashedInFlightFromNewData - Deleting failed create from dataset");
                                this.dataRecords.remove(crashedUpdate.get("uid"));
                            } else if (crashedAction != null && (crashedAction.equals("update") || crashedAction.equals("delete"))) {
                                log.d(LOG_TAG, "updateCrashedInFlightFromNewData - Reverting failed %@ in dataset" + crashedAction);
                                this.dataRecords.put(crashedUpdate.getString("uid"), pendingRecord.getPreData());
                            }
                        }

                        keysToRemove.add(pendingHash);
                        if ("applied".equals(crashedUpdate.opt("type"))) {
                            doNotify(crashedUpdate.getString("uid"), NotificationMessage.REMOTE_UPDATE_APPLIED_CODE, crashedUpdate.toString());
                        } else if ("failed".equals(crashedUpdate.opt("type"))) {
                            doNotify(crashedUpdate.getString("uid"), NotificationMessage.REMOTE_UPDATE_FAILED_CODE, crashedUpdate.toString());
                        } else if ("collisions".equals(crashedUpdate.opt("type"))) {
                            doNotify(crashedUpdate.getString("uid"), NotificationMessage.COLLISION_DETECTED_CODE, crashedUpdate.toString());
                        }

                    } else {
                        // No word on our crashed update - increment a counter to reflect another sync
                        // that did not give us
                        // any update on our crashed record.
                        pendingRecord.incrementCrashCount();
                    }
                } else {
                    // No word on our crashed update - increment a counter to reflect another sync that
                    // did not give us
                    // any update on our crashed record.
                    pendingRecord.incrementCrashCount();
                }

            }

        }
        for (String keyToRemove : keysToRemove) {
            this.pendingRecords.remove(keyToRemove);
        }
        keysToRemove.clear();

        for (Map.Entry<String, FHSyncPendingRecord> pendingRecordEntry : pendingRecords.entrySet()) {
            FHSyncPendingRecord pendingRecord = pendingRecordEntry.getValue();
            String pendingHash = pendingRecordEntry.getKey();

            if (pendingRecord.isInFlight() && pendingRecord.isCrashed()) {
                if (pendingRecord.getCrashedCount() > syncConfig.getCrashCountWait()) {
                    log.d(LOG_TAG, "updateCrashedInFlightFromNewData - Crashed inflight pending record has " + "reached crashed_count_wait limit : " + pendingRecord);
                    if (syncConfig.isResendCrashedUpdates()) {
                        log.d(LOG_TAG, "updateCrashedInFlightFromNewData - Retryig crashed inflight pending record");
                        pendingRecord.setCrashed(false);
                        pendingRecord.setInFlight(false);
                    } else {
                        log.d(LOG_TAG, "updateCrashedInFlightFromNewData - Deleting crashed inflight pending record");
                        keysToRemove.add(pendingHash);
                    }
                }
            } else if (!pendingRecord.isInFlight() && pendingRecord.isCrashed()) {
                log.d(LOG_TAG, "updateCrashedInFlightFromNewData - Trying to resolve issues with crashed non in flight record - uid =" + pendingRecord.getUid());
                // Stalled pending record because a previous pending update on the same record crashed
                JSONObject dict = resolvedCrashed.optJSONObject(pendingRecord.getUid());
                if (null != dict) {
                    log.d(LOG_TAG, String.format("updateCrashedInFlightFromNewData - Found a stalled pending record backed " + "up behind a resolved crash uid=%s :: hash=%s", pendingRecord.getUid(),
                            pendingRecord.getHashValue()));
                    pendingRecord.setCrashed(false);
                }
            }

        }

        for (String keyToRemove : keysToRemove) {
            this.pendingRecords.remove(keyToRemove);
        }

        keysToRemove.clear();
    }

    private void markInFlightAsCrashed() {
        Map<String, FHSyncPendingRecord> crashedRecords = new HashMap<>();
        for (Map.Entry<String, FHSyncPendingRecord> entry : pendingRecords.entrySet()) {
            FHSyncPendingRecord pendingRecord = entry.getValue();
            String pendingHash = entry.getKey();
            if (pendingRecord.isInFlight()) {
                log.d(LOG_TAG, "Marking in flight pending record as crashed : " + pendingHash);
                pendingRecord.setCrashed(true);
                crashedRecords.put(pendingRecord.getUid(), pendingRecord);
            }
        }

    }

    public void syncCompleteWithCode(String code) {
        syncRunning = false;
        syncEnd = new Date();
        try {
            writeToStorage();
        } catch (JSONException e) {
            log.e(LOG_TAG, "syncComplete: JSON serialization exception", e);
        }
        doNotify(hashvalue, NotificationMessage.SYNC_COMPLETE_CODE, code);
    }

    private FHSyncPendingRecord addPendingObject(String uid, JSONObject data, String action) throws JSONException {
        if (!networkClient.isOnline()) {
            doNotify(uid, NotificationMessage.OFFLINE_UPDATE_CODE, action);
        }
        FHSyncPendingRecord pending = new FHSyncPendingRecord();
        pending.setInFlight(false);
        pending.setAction(action);

        if (data != null) {
            FHSyncDataRecord dataRecord = new FHSyncDataRecord(data);
            pending.setPostData(dataRecord);
        }

        if ("create".equalsIgnoreCase(action)) {
            pending.setUid(pending.getHashValue());
            storePendingObj(pending);
        } else {
            FHSyncDataRecord existingData = dataRecords.get(uid);
            if (existingData != null) {
                pending.setUid(uid);
                pending.setPreData(existingData.clone());
                storePendingObj(pending);
            }
        }
        return pending;
    }

    private void storePendingObj(FHSyncPendingRecord pendingObj) throws JSONException {
        pendingRecords.put(pendingObj.getHashValue(), pendingObj);
        updateDatasetFromLocal(pendingObj);
        if (syncConfig.isAutoSyncLocalUpdates()) {
            syncPending = true;
        }
        writeToStorage();
        doNotify(pendingObj.getUid(), NotificationMessage.LOCAL_UPDATE_APPLIED_CODE, pendingObj.getAction());
    }

    private void updateDatasetFromLocal(FHSyncPendingRecord pendingObj) throws JSONException {
        String previousPendingUid;
        FHSyncPendingRecord previousPendingObj;
        String uid = pendingObj.getUid();
        String uidToSave = pendingObj.getHashValue();
        log.d(LOG_TAG, "updating local dataset for uid " + uid + " - action = " + pendingObj.getAction());
        JSONObject metadata = metaData.optJSONObject(uid);
        if (metadata == null) {
            metadata = new JSONObject();
            metaData.put(uid, metadata);
        }
        FHSyncDataRecord existing = dataRecords.get(uid);
        boolean fromPending = metadata.optBoolean("fromPending");

        if ("create".equalsIgnoreCase(pendingObj.getAction())) {
            if (existing != null) {
                log.d(LOG_TAG, "dataset already exists for uid for create :: " + existing.toString());
                if (fromPending) {
                    // We are trying to create on top of an existing pending record
                    // Remove the previous pending record and use this one instead
                    previousPendingUid = metadata.optString("pendingUid", null);
                    if (previousPendingUid != null) {
                        pendingRecords.remove(previousPendingUid);
                    }
                }
            }
            dataRecords.put(uid, new FHSyncDataRecord());
        }

        if ("update".equalsIgnoreCase(pendingObj.getAction())) {
            if (existing != null) {
                if (fromPending) {
                    log.d(LOG_TAG, "Updating an existing pending record for dataset :: " + existing.toString());
                    // We are trying to update an existing pending record
                    previousPendingUid = metadata.optString("pendingUid", null);
                    metadata.put("previousPendingUid", previousPendingUid);
                    if (previousPendingUid != null) {
                        previousPendingObj = pendingRecords.get(previousPendingUid);
                        if (previousPendingObj != null) {
                            if (!previousPendingObj.isInFlight()) {
                                log.d(LOG_TAG, "existing pre-flight pending record = " + previousPendingObj);
                                // We are trying to perform an update on an existing pending record
                                // modify the original record to have the latest value and delete the pending update
                                previousPendingObj.setPostData(pendingObj.getPostData());
                                pendingRecords.remove(pendingObj.getHashValue());
                                uidToSave = previousPendingUid;
                            } else if (!previousPendingObj.getHashValue().equals(pendingObj.getHashValue())) {
                                //Don't make a delayed update wait for itself, that is just rude
                                pendingObj.setDelayed(true);
                                pendingObj.setWaitingFor(previousPendingObj.getHashValue());
                            }
                        }
                    }
                }
            }
        }

        if ("delete".equalsIgnoreCase(pendingObj.getAction())) {
            if (existing != null && fromPending) {
                log.d(LOG_TAG, "Deleting an existing pending record for dataset :: " + existing);
                // We are trying to delete an existing pending record
                previousPendingUid = metadata.optString("pendingUid", null);
                metadata.put("previousPendingUid", previousPendingUid);
                if (previousPendingUid != null) {
                    previousPendingObj = pendingRecords.get(previousPendingUid);
                    if (previousPendingObj != null) {
                        if (!previousPendingObj.isInFlight()) {
                            log.d(LOG_TAG, "existing pending record = " + previousPendingObj);
                            if ("create".equalsIgnoreCase(previousPendingObj.getAction())) {
                                // We are trying to perform a delete on an existing pending create
                                // These cancel each other out so remove them both
                                pendingRecords.remove(pendingObj.getHashValue());
                                pendingRecords.remove(previousPendingUid);
                            }
                            if ("update".equalsIgnoreCase(previousPendingObj.getAction())) {
                                // We are trying to perform a delete on an existing pending update
                                // Use the pre value from the pending update for the delete and
                                // get rid of the pending update
                                pendingObj.setPreData(previousPendingObj.getPreData());
                                pendingObj.setInFlight(false);
                                pendingRecords.remove(previousPendingUid);
                            } else if (!previousPendingObj.getHashValue().equals(pendingObj.getHashValue())) {
                                //Don't make a delayed update wait for itself, that is just rude
                                pendingObj.setDelayed(true);
                                pendingObj.setWaitingFor(previousPendingObj.getHashValue());
                            }
                        }
                    }
                }

            }
            dataRecords.remove(uid);
        }

        if (dataRecords.containsKey(uid)) {
            FHSyncDataRecord record = pendingObj.getPostData();
            dataRecords.put(uid, record);
            metadata.put("fromPending", true);
            metadata.put("pendingUid", uidToSave);
        }
    }

    private void fromJSON(JSONObject obj) throws JSONException {
        JSONObject syncConfigJson = obj.getJSONObject(KEY_SYNC_CONFIG);
        this.syncConfig = FHSyncConfig.fromJSON(syncConfigJson);
        this.hashvalue = obj.optString(KEY_HASHVALUE, null);
        JSONObject pendingJSON = obj.getJSONObject(KEY_PENDING_RECORDS);
        for (Iterator<String> it = pendingJSON.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONObject pendObjJson = pendingJSON.getJSONObject(key);
            FHSyncPendingRecord pending = FHSyncPendingRecord.fromJSON(pendObjJson);
            this.pendingRecords.put(key, pending);
        }
        JSONObject dataJSON = obj.getJSONObject(KEY_DATA_RECORDS);
        for (Iterator<String> dit = dataJSON.keys(); dit.hasNext(); ) {
            String dkey = dit.next();
            JSONObject dataObjJson = dataJSON.getJSONObject(dkey);
            FHSyncDataRecord datarecord = FHSyncDataRecord.fromJSON(dataObjJson);
            this.dataRecords.put(dkey, datarecord);
        }
        if (obj.has(KEY_SYNC_LOOP_START)) {
            this.syncStart = new Date(obj.getLong(KEY_SYNC_LOOP_START));
        }
        if (obj.has(KEY_SYNC_LOOP_END)) {
            this.syncEnd = new Date(obj.getLong(KEY_SYNC_LOOP_END));
        }
        if (obj.has(KEY_ACKNOWLEDGEMENTS)) {
            this.acknowledgements = obj.getJSONArray(KEY_ACKNOWLEDGEMENTS);
        }
        if (obj.has(KEY_QUERY_PARAMS)) {
            this.queryParams = obj.getJSONObject(KEY_QUERY_PARAMS);
        }
        if (obj.has(KEY_METADATA)) {
            this.metaData = obj.getJSONObject(KEY_METADATA);
        }
    }

    private void readFromStorage() {
        try {
            String content = new String(storage.getContent(datasetId), UTF_8);
            JSONObject json = new JSONObject(content);
            fromJSON(json);
            doNotify(null, NotificationMessage.LOCAL_UPDATE_APPLIED_CODE, "load");
        } catch (FileNotFoundException e) {
            log.w(LOG_TAG, "File not found for reading, datasetId: " + datasetId);
        } catch (IOException e) {
            log.e(LOG_TAG, "Error reading from storage, datasetId: " + datasetId, e);
        } catch (JSONException je) {
            log.e(LOG_TAG, "Failed to parse JSON file , datasetId: " + datasetId, je);
        }
    }

    synchronized void writeToStorage() throws JSONException {
        try {
            String content = getJSON().toString();
            storage.putContent(datasetId, content.getBytes(UTF_8));
        } catch (FileNotFoundException ex) {
            log.e(LOG_TAG, "File not found for writing, datasetId: " + datasetId, ex);
            doNotify(null, NotificationMessage.CLIENT_STORAGE_FAILED_CODE, ex.getMessage());
        } catch (IOException e) {
            log.e(LOG_TAG, "Error writing to storage, datasetId: " + datasetId, e);
            doNotify(null, NotificationMessage.CLIENT_STORAGE_FAILED_CODE, e.getMessage());
        }
    }

    private void doNotify(String uid, int code, String message) {
        boolean sendMessage = false;
        switch (code) {
            case NotificationMessage.SYNC_STARTED_CODE:
                if (syncConfig.isNotifySyncStarted()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.SYNC_COMPLETE_CODE:
                if (syncConfig.isNotifySyncComplete()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.OFFLINE_UPDATE_CODE:
                if (syncConfig.isNotifyOfflineUpdate()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.COLLISION_DETECTED_CODE:
                if (syncConfig.isNotifySyncCollisions()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.REMOTE_UPDATE_FAILED_CODE:
                if (syncConfig.isNotifyUpdateFailed()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.REMOTE_UPDATE_APPLIED_CODE:
                if (syncConfig.isNotifyRemoteUpdateApplied()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.LOCAL_UPDATE_APPLIED_CODE:
                if (syncConfig.isNotifyLocalUpdateApplied()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.DELTA_RECEIVED_CODE:
                if (syncConfig.isNotifyDeltaReceived()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.SYNC_FAILED_CODE:
                if (syncConfig.isNotifySyncFailed()) {
                    sendMessage = true;
                }
                break;
            case NotificationMessage.CLIENT_STORAGE_FAILED_CODE:
                if (syncConfig.isNotifyClientStorageFailed()) {
                    sendMessage = true;
                }
            default:
                break;
        }
        if (sendMessage) {
            NotificationMessage notification = NotificationMessage.getMessage(datasetId, uid, code, message);
            Message msg = notificationHandler.obtainMessage(code, notification);
            notificationHandler.sendMessage(msg);
        }
    }

    /**
     * If the records returned from syncRecord request contains elements in pendings,
     * it means there are local changes that haven't been applied to the cloud yet.
     * Remove those records from the response to make sure local data will not be
     * overridden (blinking disappear / reappear effect).
     */
    private void applyPendingChangesToRecords(JSONObject resData) {
        log.d(LOG_TAG, String.format("SyncRecords result = %s pending = %s", resData.toString(), pendingRecords.toString()));
        for (FHSyncPendingRecord pendingRecord : pendingRecords.values()) {
            JSONObject resRecord = null;
            if (resData.has("create")) {
                resRecord = resData.optJSONObject("create");
                if (resRecord != null && resRecord.has(pendingRecord.getUid())) {
                    resRecord.remove(pendingRecord.getUid());
                }
            }

            if (resData.has("update")) {
                resRecord = resData.optJSONObject("update");
                if (resRecord != null && resRecord.has(pendingRecord.getUid())) {
                    resRecord.remove(pendingRecord.getUid());
                }
            }

            if (resData.has("delete")) {
                resRecord = resData.optJSONObject("delete");
                if (resRecord != null && resRecord.has(pendingRecord.getUid())) {
                    resRecord.remove(pendingRecord.getUid());
                }
            }
            log.d(LOG_TAG, String.format("SyncRecords result after pending removed = %s", resData.toString()));
        }
    }

    private void updateDelayedFromNewData(JSONObject responseData) throws JSONException {
        for (Map.Entry<String, FHSyncPendingRecord> record : this.pendingRecords.entrySet()) {

            FHSyncPendingRecord pendingObject = record.getValue();
            if (pendingObject.isDelayed() && pendingObject.getWaitingFor() != null) {
                if (responseData.has("updates")) {
                    JSONObject updatedHashes = responseData.getJSONObject("updates").optJSONObject("hashes");
                    if (updatedHashes != null && updatedHashes.has(pendingObject.getWaitingFor())) {
                        pendingObject.setDelayed(false);
                        pendingObject.setWaitingFor(null);
                    }
                    if (updatedHashes == null) {
                        boolean waitingForIsStillPending = false;
                        String waitingFor = pendingObject.getWaitingFor();
                        if (pendingObject.getWaitingFor().equals(pendingObject.getHashValue())) {
                            //Somehow a pending object is waiting on itself, lets not do that
                            pendingObject.setDelayed(false);
                            pendingObject.setWaitingFor(null);
                        } else {
                            for (FHSyncPendingRecord pending : pendingRecords.values()) {

                                if (pending.getHashValue().equals(waitingFor) || pending.getUid().equals(waitingFor)) {

                                    waitingForIsStillPending = true;
                                    break;
                                }
                            }
                            if (!waitingForIsStillPending) {
                                pendingObject.setDelayed(false);
                                pendingObject.setWaitingFor(null);
                            }
                        }
                    }
                }
            } else if (pendingObject.isDelayed() && pendingObject.getWaitingFor() == null) {
                pendingObject.setDelayed(false);
            }
        }
    }

    private void updateMetaFromNewData(JSONObject responseData) {
        Iterator keysIter = this.metaData.keys();
        Set<String> keysToRemove = new HashSet<>(this.metaData.length());
        while (keysIter.hasNext()) {
            String key = (String) keysIter.next();
            JSONObject metaData = this.metaData.optJSONObject(key);
            JSONObject updates = responseData.optJSONObject("updates");
            if (updates != null) {
                JSONObject updatedHashes = updates.optJSONObject("hashes");
                String pendingHash = metaData.optString("pendingUid");
                if (pendingHash != null && updatedHashes != null && updatedHashes.has(pendingHash)) {
                    keysToRemove.add(key);
                }
            }

        }

        for (String keyToRemove : keysToRemove) {
            metaData.remove(keyToRemove);
        }

    }

    private void checkUidChanges(JSONObject appliedUpdates) throws JSONException {
        if (appliedUpdates != null && appliedUpdates.length() > 0) {
            Iterator keysIterator = appliedUpdates.keys();
            Map<String, String> newUids = new HashMap<>();
            List<String> keys = new ArrayList<>();
            while (keysIterator.hasNext()) {
                keys.add((String) keysIterator.next());
            }

            for (String key : keys) {
                JSONObject obj = appliedUpdates.getJSONObject(key);
                String action = obj.getString("action");
                if ("create".equalsIgnoreCase(action)) {
                    String newUid = obj.getString("uid");
                    String oldUid = obj.getString("hash");
                    //remember the mapping
                    this.uidMappings.put(oldUid, newUid);
                    newUids.put(oldUid, newUid);
                    //we should update the data records to make sure they are now using the new UID
                    FHSyncDataRecord dataRecord = this.dataRecords.get(oldUid);
                    if (dataRecord != null) {
                        this.dataRecords.put(newUid, dataRecord);
                        this.dataRecords.remove(oldUid);
                    }

                }

                if (newUids.size() > 0) {
                    //we need to check all existing pendingRecords and update their UIDs if they are still the old values
                    for (Map.Entry<String, FHSyncPendingRecord> keyRecord : pendingRecords.entrySet()) {
                        FHSyncPendingRecord pendingRecord = keyRecord.getValue();
                        String pendingRecordUid = pendingRecord.getUid();
                        String newUID = newUids.get(pendingRecordUid);
                        if (newUID != null) {
                            pendingRecord.setUid(newUID);
                        }
                    }

                }

            }

        }
    }

    public void setSyncRunning(boolean syncRunning) {
        this.syncRunning = syncRunning;
    }

    public boolean isSyncRunning() {
        return syncRunning;
    }

    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    public void setSyncPending(boolean syncPending) {
        this.syncPending = syncPending;
    }

    public boolean isSyncPending() {
        return syncPending;
    }

    public void setSyncConfig(FHSyncConfig syncConfig) {
        this.syncConfig = syncConfig;
    }

    public FHSyncConfig getSyncConfig() {
        return syncConfig;
    }

    public void setQueryParams(JSONObject queryParams) {
        this.queryParams = queryParams;
    }

    public void stopSync(boolean stopSync) {
        this.stopSync = stopSync;
    }

    public boolean isStopSync() {
        return stopSync;
    }

    public Date getSyncStart() {
        return syncStart;
    }

    public Date getSyncEnd() {
        return syncEnd;
    }

    public void setNotificationHandler(FHSyncNotificationHandler handler) {
        notificationHandler = handler;
    }

    public String getDatasetId() {
        return datasetId;
    }
}
