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
package com.feedhenry.sdk.network;

import org.json.JSONObject;

/**
 * Network client interface that is used to perform HTTP requests with given sync dataset.
 */
public interface NetworkClient {

    /**
     * Performs HTTP post request against sync cloud app.
     *
     * @param datasetName sync dataset name
     * @param params      JSON params passed to the call.
     * @param callback   response callback
     */
    void performRequest(String datasetName, JSONObject params, SyncNetworkCallback callback);

    /**
     * @return Returns true if client is currently connected to network.
     */
    boolean isOnline();

    /**
     * Sets URL endpoint for the client
     *
     * @param cloudURL url
     */
    void setCloudURL(String cloudURL);

    /**
     * Registers network client for checking if online status changes.
     */
    void registerNetworkListener();

    /**
     * Registers network client for checking if online status changes.
     */
    void unregisterNetworkListener();

}
