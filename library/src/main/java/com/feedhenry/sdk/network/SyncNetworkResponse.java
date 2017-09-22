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
package com.feedhenry.sdk.network;

import org.json.JSONObject;

/**
 * Represents the response data from Sync
 */

public class SyncNetworkResponse {

    private JSONObject results;
    private Throwable error;
    private String errorMessage;

    public SyncNetworkResponse(Throwable e, String error){
        this.error = e;
        errorMessage = error;
    }

    public SyncNetworkResponse(JSONObject results) {
        this.results = results;
    }

    /**
     * Gets the response data as a JSONObject.
     *
     * @return a JSONObject
     */
    public JSONObject getJson() {
        return results;
    }

    /**
     * Gets the error.
     *
     * @return the error
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the raw response content.
     *
     * @return the raw response content
     */
    public String getRawResponse() {
        if (results != null) {
            return results.toString();
        } else {
            return errorMessage;
        }
    }
}
