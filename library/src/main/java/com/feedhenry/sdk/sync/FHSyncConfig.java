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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configuration options for the sync framework.
 */
public class FHSyncConfig {

    private int syncFrequencySeconds = 10;
    private boolean autoSyncLocalUpdates = false;
    private boolean notifySyncStarted = false;
    private boolean notifySyncComplete = false;
    private boolean notifySyncCollisions = false;
    private boolean notifyOfflineUpdate = false;
    private boolean notifyRemoteUpdateFailed = false;
    private boolean notifyRemoteUpdateApplied = false;
    private boolean notifyLocalUpdateApplied = false;
    private boolean notifyDeltaReceived = false;
    private boolean notifySyncFailed = false;
    private boolean notifyClientStorageFailed = false;
    private int crashCountWait = 10;
    private boolean resendCrashedUpdates = true;
    private boolean useCustomSync = false;

    private static final String KEY_SYNC_FREQUENCY = "syncFrequency";
    private static final String KEY_AUTO_SYNC_UPDATES = "autoSyncLocalUpdates";
    private static final String KEY_NOTIFY_CLIENT_STORAGE_FAILED = "notifyClientStorageFailed";
    private static final String KEY_NOTIFY_DELTA_RECEIVED = "notifyDeltaReceived";
    private static final String KEY_NOTIFY_OFFLINE_UPDATED = "notifyOfflineUpdated";
    private static final String KEY_NOTIFY_SYNC_COLLISION = "notifySyncCollision";
    private static final String KEY_NOTIFY_SYNC_COMPLETED = "notifySyncCompleted";
    private static final String KEY_NOTIFY_SYNC_STARTED = "notifySyncStarted";
    private static final String KEY_NOTIFY_REMOTE_UPDATED_APPLIED = "notifyRemoteUpdatedApplied";
    private static final String KEY_NOTIFY_LOCAL_UPDATE_APPLIED = "notifyLocalUpdateApplied";
    private static final String KEY_NOTIFY_REMOTE_UPDATED_FAILED = "notifyRemoteUpdateFailed";
    private static final String KEY_NOTIFY_SYNC_FAILED = "notifySyncFailed";
    private static final String KEY_CRASHCOUNT = "crashCountWait";
    private static final String KEY_RESEND_CRASH = "resendCrashdUpdates";

    /**
     * Sets the sync interval in seconds.
     *
     * @param frequencySeconds the new sync interval
     */
    @Deprecated
    public void setSyncFrequency(int frequencySeconds) {
        syncFrequencySeconds = frequencySeconds;
    }

    /**
     * Gets the current sync interval.
     *
     * @return the current sync interval.
     */
    public int getSyncFrequency() {
        return syncFrequencySeconds;
    }

    /**
     * Gets whether the sync client notifies on a sync start event.
     *
     * @return whether a sync start event will trigger a notification
     */
    public boolean isNotifySyncStarted() {
        return notifySyncStarted;
    }

    /**
     * Sets if the sync client should notify on a sync start event.
     *
     * @param notifySyncStarted whether to notify on sync start
     */
    @Deprecated
    public void setNotifySyncStarted(boolean notifySyncStarted) {
        this.notifySyncStarted = notifySyncStarted;
    }

    /**
     * Gets whether the sync client notifies on a sync complete event.
     *
     * @return whether a sync complete event will trigger a notification
     */
    public boolean isNotifySyncComplete() {
        return notifySyncComplete;
    }

    /**
     * Sets if the sync client should notify on a sync complete event.
     *
     * @param notifySyncComplete whether to notify on sync complete
     */
    @Deprecated
    public void setNotifySyncComplete(boolean notifySyncComplete) {
        this.notifySyncComplete = notifySyncComplete;
    }

    /**
     * Gets whether the sync client notifies on a sync collision event.
     *
     * @return whether a sync collision event will trigger a notification
     */
    public boolean isNotifySyncCollisions() {
        return notifySyncCollisions;
    }

    /**
     * Sets if the sync client should notify on a sync collision event.
     *
     * @param notifySyncCollsion whether to notify on sync collision
     */
    @Deprecated
    public void setNotifySyncCollisions(boolean notifySyncCollsion) {
        this.notifySyncCollisions = notifySyncCollsion;
    }

    /**
     * Gets whether the sync client notifies on an offline update event.
     *
     * @return whether an offline update event will trigger a notification
     */
    public boolean isNotifyOfflineUpdate() {
        return notifyOfflineUpdate;
    }

    /**
     * Sets if the sync client should notify on an offline update event.
     *
     * @param notifyOfflineUpdate whether to notify on offline update
     */
    @Deprecated
    public void setNotifyOfflineUpdate(boolean notifyOfflineUpdate) {
        this.notifyOfflineUpdate = notifyOfflineUpdate;
    }

    /**
     * Gets whether the sync client notifies on an update failed event.
     *
     * @return whether an update failed event will trigger a notification
     */
    public boolean isNotifyUpdateFailed() {
        return notifyRemoteUpdateFailed;
    }

    /**
     * Sets if the sync client should notify on an update failed event.
     *
     * @param notifyUpdateFailed whether to notify on update failed
     */
    @Deprecated
    public void setNotifyUpdateFailed(boolean notifyUpdateFailed) {
        this.notifyRemoteUpdateFailed = notifyUpdateFailed;
    }

    /**
     * Gets whether the sync client notifies on a remote update applied event.
     *
     * @return whether a remote update applied event will trigger a notification
     */
    public boolean isNotifyRemoteUpdateApplied() {
        return notifyRemoteUpdateApplied;
    }

    /**
     * Sets if the sync client should notify on a remote updates applied event.
     *
     * @param notifyRemoteUpdateApplied whether to notify on remote updates applied
     */
    @Deprecated
    public void setNotifyRemoteUpdateApplied(boolean notifyRemoteUpdateApplied) {
        this.notifyRemoteUpdateApplied = notifyRemoteUpdateApplied;
    }

    /**
     * Gets whether the sync client notifies on a local updates applied event.
     *
     * @return whether a local updates applied event will trigger a notification
     */
    public boolean isNotifyLocalUpdateApplied() {
        return notifyLocalUpdateApplied;
    }

    /**
     * Sets if the sync client should notify on a local updates applied event.
     *
     * @param notifyLocalUpdateApplied whether to notify on local updates applied
     */
    @Deprecated
    public void setNotifyLocalUpdateApplied(boolean notifyLocalUpdateApplied) {
        this.notifyLocalUpdateApplied = notifyLocalUpdateApplied;
    }

    /**
     * Gets whether the sync client notifies on a delta received event.
     *
     * @return whether a delta received event will trigger a notification
     */
    public boolean isNotifyDeltaReceived() {
        return notifyDeltaReceived;
    }

    /**
     * Sets if the sync client should notify on a delta received event.
     *
     * @param notifyDeltaReceived whether to notify on delta received
     */
    @Deprecated
    public void setNotifyDeltaReceived(boolean notifyDeltaReceived) {
        this.notifyDeltaReceived = notifyDeltaReceived;
    }

    /**
     * Gets whether the sync client notifies on a sync failed event.
     *
     * @return whether a sync failed event will trigger a notification
     */
    public boolean isNotifySyncFailed() {
        return notifySyncFailed;
    }

    /**
     * Sets if the sync client should notify on a sync failed event.
     *
     * @param notifySyncFailed whether to notify on sync failed
     */
    @Deprecated
    public void setNotifySyncFailed(boolean notifySyncFailed) {
        this.notifySyncFailed = notifySyncFailed;
    }

    /**
     * Sets if the sync client should notify on a client storage failed event.
     *
     * @param notifyClientStorageFailed whether to notify on client storage failed
     */
    @Deprecated
    public void setNotifyClientStorageFailed(boolean notifyClientStorageFailed) {
        this.notifyClientStorageFailed = notifyClientStorageFailed;
    }

    /**
     * Gets whether the sync client notifies on a client storage failed event.
     *
     * @return whether a client storage failed event will trigger a notification
     */
    public boolean isNotifyClientStorageFailed() {
        return this.notifyClientStorageFailed;
    }

    /**
     * Gets whether the sync client automatically updates on local changes.
     *
     * @return whether local changes are automatically synced
     */
    public boolean isAutoSyncLocalUpdates() {
        return autoSyncLocalUpdates;
    }

    /**
     * Sets if the sync client should automatically update on local changes.
     *
     * @param autoSyncLocalUpdates whether local changes should automatically sync
     */
    @Deprecated
    public void setAutoSyncLocalUpdates(boolean autoSyncLocalUpdates) {
        this.autoSyncLocalUpdates = autoSyncLocalUpdates;
    }

    /**
     * Gets the maximum crash count.
     *
     * @return the maximum crash count number
     */
    public int getCrashCountWait() {
        return crashCountWait;
    }

    /**
     * Sets the maximum crash count number.
     * Changes may fail to be applied (crash) due to various reasons (e.g., network issues).
     * If the crash count reaches this limit, the changes will be either re-submitted or abandoned.
     *
     * @param crashCountWait the crash limit
     */
    @Deprecated
    public void setCrashCountWait(int crashCountWait) {
        this.crashCountWait = crashCountWait;
    }

    /**
     * Gets whether changes should be re-submitted or abandoned when the crash limit is reached.
     *
     * @return true or false
     */
    public boolean isResendCrashedUpdates() {
        return resendCrashedUpdates;
    }

    /**
     * Sets whether changes should be re-submitted once the crash limit is reached.
     * If false, changes will be discarded.
     *
     * @param resendCrashedUpdates true or false.
     */
    @Deprecated
    public void setResendCrashedUpdates(boolean resendCrashedUpdates) {
        this.resendCrashedUpdates = resendCrashedUpdates;
    }

    /**
     * Set if legacy mode is used
     *
     * @param useCustomSync
     */
    @Deprecated
    public void setUseCustomSync(boolean useCustomSync) {
        this.useCustomSync = useCustomSync;
    }

    /**
     * Check if legacy mode is enabled
     *
     * @return
     */
    public boolean useCustomSync() {
        return this.useCustomSync;
    }

    ;

    /**
     * Deprecated constructor, use {@link Builder} instead.
     */
    public FHSyncConfig() {

    }

    /**
     * Gets a JSON representation of the configuration object.
     *
     * @return The JSON object
     */
    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();
        try {
            ret.put(KEY_SYNC_FREQUENCY, this.syncFrequencySeconds);
            ret.put(KEY_AUTO_SYNC_UPDATES, this.autoSyncLocalUpdates);
            ret.put(KEY_NOTIFY_CLIENT_STORAGE_FAILED, this.notifyClientStorageFailed);
            ret.put(KEY_NOTIFY_DELTA_RECEIVED, this.notifyDeltaReceived);
            ret.put(KEY_NOTIFY_OFFLINE_UPDATED, this.notifyOfflineUpdate);
            ret.put(KEY_NOTIFY_SYNC_COLLISION, this.notifySyncCollisions);
            ret.put(KEY_NOTIFY_SYNC_COMPLETED, this.notifySyncComplete);
            ret.put(KEY_NOTIFY_SYNC_STARTED, this.notifySyncStarted);
            ret.put(KEY_NOTIFY_REMOTE_UPDATED_APPLIED, this.notifyRemoteUpdateApplied);
            ret.put(KEY_NOTIFY_LOCAL_UPDATE_APPLIED, this.notifyLocalUpdateApplied);
            ret.put(KEY_NOTIFY_REMOTE_UPDATED_FAILED, this.notifyRemoteUpdateFailed);
            ret.put(KEY_NOTIFY_SYNC_FAILED, this.notifySyncFailed);
            ret.put(KEY_CRASHCOUNT, this.crashCountWait);
            ret.put(KEY_RESEND_CRASH, this.resendCrashedUpdates);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    /**
     * Creates a new configuration object from JSON.
     *
     * @param obj the sync config JSON
     *
     * @return the new sync config object
     *
     * @deprecated use {@link Builder#fromJSON(JSONObject)}
     */
    @Deprecated
    public static FHSyncConfig fromJSON(JSONObject obj) {
        FHSyncConfig config = new FHSyncConfig();
        config.setSyncFrequency(obj.optInt(KEY_SYNC_FREQUENCY));
        config.setAutoSyncLocalUpdates(obj.optBoolean(KEY_AUTO_SYNC_UPDATES));
        config.setNotifyClientStorageFailed(obj.optBoolean(KEY_NOTIFY_CLIENT_STORAGE_FAILED));
        config.setNotifyDeltaReceived(obj.optBoolean(KEY_NOTIFY_DELTA_RECEIVED));
        config.setNotifyOfflineUpdate(obj.optBoolean(KEY_NOTIFY_OFFLINE_UPDATED));
        config.setNotifySyncCollisions(obj.optBoolean(KEY_NOTIFY_SYNC_COLLISION));
        config.setNotifySyncComplete(obj.optBoolean(KEY_NOTIFY_SYNC_COMPLETED));
        config.setNotifySyncStarted(obj.optBoolean(KEY_NOTIFY_SYNC_STARTED));
        config.setNotifyRemoteUpdateApplied(obj.optBoolean(KEY_NOTIFY_REMOTE_UPDATED_APPLIED));
        config.setNotifyLocalUpdateApplied(obj.optBoolean(KEY_NOTIFY_LOCAL_UPDATE_APPLIED));
        config.setNotifyUpdateFailed(obj.optBoolean(KEY_NOTIFY_REMOTE_UPDATED_FAILED));
        config.setNotifySyncFailed(obj.optBoolean(KEY_NOTIFY_SYNC_FAILED));
        config.setCrashCountWait(obj.optInt(KEY_CRASHCOUNT, 10));
        config.setResendCrashedUpdates(obj.optBoolean(KEY_RESEND_CRASH));
        return config;
    }

    @Deprecated
    public FHSyncConfig clone() {
        JSONObject json = this.toJSON();
        return FHSyncConfig.fromJSON(json);
    }

    /**
     * Class for creating FHSyncConfig.
     */
    public static class Builder {

        private final FHSyncConfig instance = new FHSyncConfig();

        /**
         * Sets the sync interval in seconds.
         *
         * @param syncFrequencySeconds the new sync interval
         */
        public Builder syncFrequencySeconds(int syncFrequencySeconds) {
            instance.syncFrequencySeconds = syncFrequencySeconds;
            return this;
        }

        /**
         * Sets if the sync client should automatically update on local changes.
         *
         * @param autoSyncLocalUpdates whether local changes should automatically sync
         */
        public Builder autoSyncLocalUpdates(boolean autoSyncLocalUpdates) {
            instance.autoSyncLocalUpdates = autoSyncLocalUpdates;
            return this;
        }

        /**
         * Sets the maximum crash count number.
         * Changes may fail to be applied (crash) due to various reasons (e.g., network issues).
         * If the crash count reaches this limit, the changes will be either re-submitted or abandoned.
         *
         * @param crashCountWait the crash limit
         */
        public Builder crashCountWait(int crashCountWait) {
            instance.crashCountWait = crashCountWait;
            return this;
        }

        /**
         * Sets whether changes should be re-submitted once the crash limit is reached.
         * If false, changes will be discarded.
         *
         * @param resendCrashedUpdates true or false.
         */
        public Builder resendCrashedUpdates(boolean resendCrashedUpdates) {
            instance.resendCrashedUpdates = resendCrashedUpdates;
            return this;
        }

        /**
         * Set if legacy mode is used
         *
         * @param useCustomSync
         */
        public Builder useCustomSync(boolean useCustomSync) {
            instance.useCustomSync = useCustomSync;
            return this;
        }

        /**
         * Sets if the sync client should notify on a sync start event.
         *
         * @param notifySyncStarted whether to notify on sync start
         */
        public Builder notifySyncStarted(boolean notifySyncStarted) {
            instance.notifySyncStarted = notifySyncStarted;
            return this;
        }

        /**
         * Sets if the sync client should notify on a sync complete event.
         *
         * @param notifySyncComplete whether to notify on sync complete
         */
        public Builder notifySyncComplete(boolean notifySyncComplete) {
            instance.notifySyncComplete = notifySyncComplete;
            return this;
        }

        /**
         * Sets if the sync client should notify on a sync collision event.
         *
         * @param notifySyncCollsion whether to notify on sync collision
         */
        public Builder notifySyncCollisions(boolean notifySyncCollsion) {
            instance.notifySyncCollisions = notifySyncCollsion;
            return this;
        }

        /**
         * Sets if the sync client should notify on an offline update event.
         *
         * @param notifyOfflineUpdate whether to notify on offline update
         */
        public Builder notifyOfflineUpdate(boolean notifyOfflineUpdate) {
            instance.notifyOfflineUpdate = notifyOfflineUpdate;
            return this;
        }

        /**
         * Sets if the sync client should notify on an update failed event.
         *
         * @param notifyUpdateFailed whether to notify on update failed
         */
        public Builder notifyUpdateFailed(boolean notifyUpdateFailed) {
            instance.notifyRemoteUpdateFailed = notifyUpdateFailed;
            return this;
        }

        /**
         * Sets if the sync client should notify on a remote updates applied event.
         *
         * @param notifyRemoteUpdateApplied whether to notify on remote updates applied
         */
        public Builder notifyRemoteUpdateApplied(boolean notifyRemoteUpdateApplied) {
            instance.notifyRemoteUpdateApplied = notifyRemoteUpdateApplied;
            return this;
        }

        /**
         * Sets if the sync client should notify on a local updates applied event.
         *
         * @param notifyLocalUpdateApplied whether to notify on local updates applied
         */
        public Builder notifyLocalUpdateApplied(boolean notifyLocalUpdateApplied) {
            instance.notifyLocalUpdateApplied = notifyLocalUpdateApplied;
            return this;
        }

        /**
         * Sets if the sync client should notify on a delta received event.
         *
         * @param notifyDeltaReceived whether to notify on delta received
         */
        public Builder notifyDeltaReceived(boolean notifyDeltaReceived) {
            instance.notifyDeltaReceived = notifyDeltaReceived;
            return this;
        }

        /**
         * Sets if the sync client should notify on a sync failed event.
         *
         * @param notifySyncFailed whether to notify on sync failed
         */
        public Builder notifySyncFailed(boolean notifySyncFailed) {
            instance.notifySyncFailed = notifySyncFailed;
            return this;
        }

        /**
         * Sets if the sync client should notify on a client storage failed event.
         *
         * @param notifyClientStorageFailed whether to notify on client storage failed
         */
        public Builder notifyClientStorageFailed(boolean notifyClientStorageFailed) {
            instance.notifyClientStorageFailed = notifyClientStorageFailed;
            return this;
        }

        /**
         * Enables all notifications for sync client.
         */
        public Builder notifyEnableAll() {
            instance.notifyClientStorageFailed = true;
            instance.notifyDeltaReceived = true;
            instance.notifyOfflineUpdate = true;
            instance.notifySyncCollisions = true;
            instance.notifySyncComplete = true;
            instance.notifySyncStarted = true;
            instance.notifyRemoteUpdateApplied = true;
            instance.notifyLocalUpdateApplied = true;
            instance.notifyRemoteUpdateFailed = true;
            instance.notifySyncFailed = true;
            return this;
        }

        /**
         * Sets builder from JSON config.
         *
         * @param pObj the sync config JSON
         */
        public Builder fromJSON(JSONObject pObj) {
            if (pObj != null) {
                instance.syncFrequencySeconds = pObj.optInt(KEY_SYNC_FREQUENCY);
                instance.autoSyncLocalUpdates = pObj.optBoolean(KEY_AUTO_SYNC_UPDATES);
                instance.notifyClientStorageFailed = pObj.optBoolean(KEY_NOTIFY_CLIENT_STORAGE_FAILED);
                instance.notifyDeltaReceived = pObj.optBoolean(KEY_NOTIFY_DELTA_RECEIVED);
                instance.notifyOfflineUpdate = pObj.optBoolean(KEY_NOTIFY_OFFLINE_UPDATED);
                instance.notifySyncCollisions = pObj.optBoolean(KEY_NOTIFY_SYNC_COLLISION);
                instance.notifySyncComplete = pObj.optBoolean(KEY_NOTIFY_SYNC_COMPLETED);
                instance.notifySyncStarted = pObj.optBoolean(KEY_NOTIFY_SYNC_STARTED);
                instance.notifyRemoteUpdateApplied = pObj.optBoolean(KEY_NOTIFY_REMOTE_UPDATED_APPLIED);
                instance.notifyLocalUpdateApplied = pObj.optBoolean(KEY_NOTIFY_LOCAL_UPDATE_APPLIED);
                instance.notifyRemoteUpdateFailed = pObj.optBoolean(KEY_NOTIFY_REMOTE_UPDATED_FAILED);
                instance.notifySyncFailed = pObj.optBoolean(KEY_NOTIFY_SYNC_FAILED);
                instance.crashCountWait = pObj.optInt(KEY_CRASHCOUNT, 10);
                instance.resendCrashedUpdates = pObj.optBoolean(KEY_RESEND_CRASH);
            }
            return this;
        }

        /**
         * Creates FHSyncConfig
         *
         * @return created config object
         */
        public FHSyncConfig build() {
            return instance;
        }
    }

}
