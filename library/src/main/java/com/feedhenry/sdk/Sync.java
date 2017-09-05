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
package com.feedhenry.sdk;

import android.content.Context;
import com.feedhenry.sdk.exceptions.InvalidUrlException;
import com.feedhenry.sdk.network.NetworkManager;
import com.feedhenry.sdk.network.SyncNetworkCallback;
import com.feedhenry.sdk.network.SyncNetworkResponse;
import okhttp3.*;
import org.json.fh.JSONObject;

import java.io.IOException;

/**
 * The Sync class provides static methods to initialize the library, create new instances of all the
 * API request objects, and configure global settings.
 */
public class Sync {

    private static boolean mReady = false;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client;

    public static final int LOG_LEVEL_VERBOSE = 1;
    public static final int LOG_LEVEL_DEBUG = 2;
    public static final int LOG_LEVEL_INFO = 3;
    public static final int LOG_LEVEL_WARNING = 4;
    public static final int LOG_LEVEL_ERROR = 5;
    public static final int LOG_LEVEL_NONE = Integer.MAX_VALUE;

    private static int mLogLevel = LOG_LEVEL_ERROR;

    public static final String VERSION = "2.1.0"; // DO NOT CHANGE, the ant build task will automatically update this value. Update it in VERSION.txt

    private static boolean mInitCalled = false;

    private static Context mContext;
    private static String cloudURL;
    private static Headers mHeaders;

    private Sync() {
    }

    public static void init(String cloudUrl, Context pContext) {
        cloudURL = cloudUrl;
        // Be sure we are store the safety application context
        mContext = pContext.getApplicationContext();
        NetworkManager.init(mContext);
        checkNetworkStatus();
        client = new OkHttpClient.Builder().build();
    }

    private static void checkNetworkStatus() {
        NetworkManager networkManager = NetworkManager.getInstance();
        networkManager.registerNetworkListener();
        networkManager.checkNetworkStatus();
        if (networkManager.isOnline() && !mReady && mInitCalled) {
            // TODO Monitor status for service
        }
    }

    public static boolean isOnline() {
        return NetworkManager.getInstance().isOnline();
    }

    public static void stop() {
        NetworkManager.getInstance().unregisterNetworkListener();
    }

    /**
     * Sets the log level for the library.
     * The default level is {@link #LOG_LEVEL_ERROR}. Please make sure this is set to {@link #LOG_LEVEL_ERROR}
     * or {@link #LOG_LEVEL_NONE} before releasing the application.
     * The log level can be one of
     * <ul>
     * <li>{@link #LOG_LEVEL_VERBOSE}</li>
     * <li>{@link #LOG_LEVEL_DEBUG}</li>
     * <li>{@link #LOG_LEVEL_INFO}</li>
     * <li>{@link #LOG_LEVEL_WARNING}</li>
     * <li>{@link #LOG_LEVEL_ERROR}</li>
     * <li>{@link #LOG_LEVEL_NONE}</li>
     * </ul>
     *
     * @param pLogLevel The level of logging for the Sync library
     */
    public static void setLogLevel(int pLogLevel) {
        mLogLevel = pLogLevel;
    }

    /**
     * Gets the current log level for the Sync library.
     *
     * @return The current log level
     */
    public static int getLogLevel() {
        return mLogLevel;
    }

    /**
     * Configures HTTP headers that will be used to perform the cloud requests.
     *
     * @param requestHeaders list of HTTP headers
     */
    public static void setHeaders(Headers requestHeaders) {
        mHeaders = requestHeaders;
    }

    public static void performRequest(String datasetName, JSONObject params, SyncNetworkCallback pCallback) {
        // TODO change params to json string.
        RequestBody body = RequestBody.create(JSON, params.toString());
        HttpUrl url = HttpUrl.parse(cloudURL);
        if (url != null) {
            url = url.newBuilder().addPathSegment(datasetName).build();
            Request request = new Request.Builder().url(url).headers(mHeaders).post(body).build();

            // TODO check if online/offline
            // TODO perform async request
            try {
                Response response = client.newCall(request).execute();
                String result = response.body().string();
                SyncNetworkResponse syncNetworkResponse = new SyncNetworkResponse(new JSONObject(result));
                pCallback.success(syncNetworkResponse);
            } catch (IOException e) {
                pCallback.fail(new SyncNetworkResponse(e, "Request failed"));
            }
        } else {
            pCallback.fail(new SyncNetworkResponse(new InvalidUrlException(cloudURL), "Invalid cloud app URL"));
        }
    }
}
