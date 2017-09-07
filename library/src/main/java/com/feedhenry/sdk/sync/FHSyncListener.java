/**
 * Copyright Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.feedhenry.sdk.sync;

import android.support.annotation.NonNull;

/**
 * Implement the listener interface to monitor events invoked by the sync framework.
 */
public interface FHSyncListener {
    /**
     * Invoked when a sync loop start event is emitted
     *
     * @param pMessage The message
     */
    void onSyncStarted(NotificationMessage pMessage);

    /**
     * Invoked when a sync loop complete event is emitted
     *
     * @param pMessage The message
     */
    void onSyncCompleted(NotificationMessage pMessage);

    /**
     * Invoked when a offline update event is emitted.
     *
     * @param pMessage The message
     */
    void onUpdateOffline(NotificationMessage pMessage);

    /**
     * Invoked when a collision event is emitted.
     *
     * @param pMessage The message
     */
    void onCollisionDetected(NotificationMessage pMessage);

    /**
     * Invoked when a remote update failed event is emitted.
     *
     * @param pMessage The message
     */
    void onRemoteUpdateFailed(NotificationMessage pMessage);

    /**
     * Invoked when a remote update event is emitted.
     *
     * @param pMessage The message
     */
    void onRemoteUpdateApplied(NotificationMessage pMessage);

    /**
     * Invoked when a local update applied event is emitted.
     *
     * @param pMessage The message
     */
    void onLocalUpdateApplied(NotificationMessage pMessage);

    /**
     * Invoked when a delta received event is emitted.
     *
     * @param pMessage The message
     */
    void onDeltaReceived(NotificationMessage pMessage);

    /**
     * Invoked when a sync failed event is emitted.
     *
     * @param pMessage The message
     */
    void onSyncFailed(NotificationMessage pMessage);

    /**
     * Invoked when a client storage failed event is emitted.
     *
     * @param pMessage The message
     */
    void onClientStorageFailed(NotificationMessage pMessage);

    /**
     * Created on 30.8.17.
     */
    class Builder {
        private FHSyncEvent onSyncStarted;
        private FHSyncEvent onSyncCompleted;
        private FHSyncEvent onUpdateOffline;
        private FHSyncEvent onCollisionDetected;
        private FHSyncEvent onRemoteUpdateFailed;
        private FHSyncEvent onRemoteUpdateApplied;
        private FHSyncEvent onLocalUpdateApplied;
        private FHSyncEvent onDeltaReceived;
        private FHSyncEvent onSyncFailed;
        private FHSyncEvent onClientStorageFailed;


        /**
         * Sets event which is invoked when a sync loop start event is emitted
         *
         * @param pOnSyncStarted event handler
         */
        public Builder onSyncStarted(@NonNull FHSyncEvent pOnSyncStarted) {
            this.onSyncStarted = pOnSyncStarted;
            return this;
        }

        /**
         * Sets event which is invoked when a sync loop complete event is emitted
         *
         * @param pOnSyncCompleted event handler
         */
        public Builder onSyncCompleted(FHSyncEvent pOnSyncCompleted) {
            this.onSyncCompleted = pOnSyncCompleted;
            return this;
        }

        /**
         * Sets event which is invoked when a offline update event is emitted.
         *
         * @param pOUpdateOffline event handler
         */
        public Builder onUpdateOffline(FHSyncEvent pOUpdateOffline) {
            this.onUpdateOffline = pOUpdateOffline;
            return this;
        }

        /**
         * Sets event which is invoked when a collision event is emitted.
         *
         * @param pOnCollisionDetected event handler
         */
        public Builder onCollisionDetected(FHSyncEvent pOnCollisionDetected) {
            this.onCollisionDetected = pOnCollisionDetected;
            return this;
        }

        /**
         * Sets event which is invoked when a remote update failed event is emitted.
         *
         * @param pOnRemoteUpdateFailed event handler
         */
        public Builder onRemoteUpdateFailed(FHSyncEvent pOnRemoteUpdateFailed) {
            this.onRemoteUpdateFailed = pOnRemoteUpdateFailed;
            return this;
        }

        /**
         * Sets event which is invoked when a remote update event is emitted.
         *
         * @param pOnRemoteUpdateApplied event handler
         */
        public Builder onRemoteUpdateApplied(FHSyncEvent pOnRemoteUpdateApplied) {
            this.onRemoteUpdateApplied = pOnRemoteUpdateApplied;
            return this;
        }

        /**
         * Sets event which is invoked when a local update applied event is emitted.
         *
         * @param pOnLocalUpdateApplied event handler
         */
        public Builder onLocalUpdateApplied(FHSyncEvent pOnLocalUpdateApplied) {
            this.onLocalUpdateApplied = pOnLocalUpdateApplied;
            return this;
        }

        /**
         * Sets event which is invoked when a delta received event is emitted.
         *
         * @param pOnDeltaReceived event handler
         */
        public Builder onDeltaReceived(FHSyncEvent pOnDeltaReceived) {
            this.onDeltaReceived = pOnDeltaReceived;
            return this;
        }

        /**
         * Sets event which is invoked when a sync failed event is emitted.
         *
         * @param pOnSyncFailed event handler
         */
        public Builder onSyncFailed(FHSyncEvent pOnSyncFailed) {
            this.onSyncFailed = pOnSyncFailed;
            return this;
        }

        /**
         * Sets event which is invoked when a client storage failed event is emitted.
         *
         * @param pOnClientStorageFailed event handler
         */
        public Builder onClientStorageFailed(FHSyncEvent pOnClientStorageFailed) {
            this.onClientStorageFailed = pOnClientStorageFailed;
            return this;
        }

        public FHSyncListener build() {
            return new FHSyncListener() {
                @Override
                public void onSyncStarted(NotificationMessage pMessage) {
                    if (onSyncStarted != null) {
                        onSyncStarted.event(pMessage);
                    }
                }

                @Override
                public void onSyncCompleted(NotificationMessage pMessage) {
                    if (onSyncCompleted != null) {
                        onSyncCompleted.event(pMessage);
                    }
                }

                @Override
                public void onUpdateOffline(NotificationMessage pMessage) {
                    if (onUpdateOffline != null) {
                        onUpdateOffline.event(pMessage);
                    }
                }

                @Override
                public void onCollisionDetected(NotificationMessage pMessage) {
                    if (onCollisionDetected != null) {
                        onCollisionDetected.event(pMessage);
                    }
                }

                @Override
                public void onRemoteUpdateFailed(NotificationMessage pMessage) {
                    if (onRemoteUpdateFailed != null) {
                        onRemoteUpdateFailed.event(pMessage);
                    }
                }

                @Override
                public void onRemoteUpdateApplied(NotificationMessage pMessage) {
                    if (onRemoteUpdateApplied != null) {
                        onRemoteUpdateApplied.event(pMessage);
                    }
                }

                @Override
                public void onLocalUpdateApplied(NotificationMessage pMessage) {
                    if (onLocalUpdateApplied != null) {
                        onLocalUpdateApplied.event(pMessage);
                    }
                }

                @Override
                public void onDeltaReceived(NotificationMessage pMessage) {
                    if (onDeltaReceived != null) {
                        onDeltaReceived.event(pMessage);
                    }
                }

                @Override
                public void onSyncFailed(NotificationMessage pMessage) {
                    if (onSyncFailed != null) {
                        onSyncFailed.event(pMessage);
                    }
                }

                @Override
                public void onClientStorageFailed(NotificationMessage pMessage) {
                    if (onClientStorageFailed != null) {
                        onClientStorageFailed.event(pMessage);
                    }
                }
            };
        }
    }
}
