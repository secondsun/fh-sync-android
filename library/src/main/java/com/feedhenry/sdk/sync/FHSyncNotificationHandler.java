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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class FHSyncNotificationHandler extends Handler {

    private FHSyncListener syncListener;

    public FHSyncNotificationHandler(FHSyncListener listener) {
        super();
        syncListener = listener;
    }

    public FHSyncNotificationHandler(Looper looper, FHSyncListener listener) {
        super(looper);
        syncListener = listener;
    }

    public void setSyncListener(FHSyncListener listener) {
        syncListener = listener;
    }

    public void handleMessage(Message msg) {
        NotificationMessage notification = (NotificationMessage) msg.obj;
        if (syncListener != null) {
            switch (msg.what) {
                case NotificationMessage.SYNC_STARTED_CODE:
                    syncListener.onSyncStarted(notification);
                    break;
                case NotificationMessage.SYNC_COMPLETE_CODE:
                    syncListener.onSyncCompleted(notification);
                    break;
                case NotificationMessage.OFFLINE_UPDATE_CODE:
                    syncListener.onUpdateOffline(notification);
                    break;
                case NotificationMessage.COLLISION_DETECTED_CODE:
                    syncListener.onCollisionDetected(notification);
                    break;
                case NotificationMessage.REMOTE_UPDATE_FAILED_CODE:
                    syncListener.onRemoteUpdateFailed(notification);
                    break;
                case NotificationMessage.REMOTE_UPDATE_APPLIED_CODE:
                    syncListener.onRemoteUpdateApplied(notification);
                    break;
                case NotificationMessage.LOCAL_UPDATE_APPLIED_CODE:
                    syncListener.onLocalUpdateApplied(notification);
                    break;
                case NotificationMessage.DELTA_RECEIVED_CODE:
                    syncListener.onDeltaReceived(notification);
                    break;
                case NotificationMessage.SYNC_FAILED_CODE:
                    syncListener.onSyncFailed(notification);
                    break;
                case NotificationMessage.CLIENT_STORAGE_FAILED_CODE:
                    syncListener.onClientStorageFailed(notification);
                default:
                    break;
            }
        }
    }
}
