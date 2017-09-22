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

    private JSONObject mResults;
    private Throwable mError;
    private String mErrorMessage;

    public SyncNetworkResponse(Throwable e, String pError){
        mError = e;
        mErrorMessage = pError;
    }

    public SyncNetworkResponse(JSONObject pResults) {
        mResults = pResults;
    }

    /**
     * Gets the response data as a JSONObject.
     *
     * @return a JSONObject
     */
    public JSONObject getJson() {
        return mResults;
    }

    /**
     * Gets the error.
     *
     * @return the error
     */
    public Throwable getError() {
        return mError;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * Gets the raw response content.
     *
     * @return the raw response content
     */
    public String getRawResponse() {
        if (mResults != null) {
            return mResults.toString();
        } else {
            return mErrorMessage;
        }
    }
}
