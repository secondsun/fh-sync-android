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

import com.feedhenry.sdk.network.NetworkManager;
import com.feedhenry.sdk.network.SyncNetworkCallback;
import com.feedhenry.sdk.network.SyncNetworkResponse;

import org.json.fh.JSONObject;


/**
 * The Sync class provides static methods to initialize the library, create new instances of all the
 * API request objects, and configure global settings.
 */
public class Sync {

    private static boolean mReady = false;

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
    private final String cloudURL;

    public Sync(String cloudUrl,Context pContext) {
        this.cloudURL = cloudUrl;
        // Be sure we are store the safety application context
        mContext = pContext.getApplicationContext();
    }

    private static void checkNetworkStatus() {
        NetworkManager networkManager = NetworkManager.init(mContext);
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

    public static SyncNetworkResponse performRequest(String datasetName, JSONObject params, SyncNetworkCallback pCallback) {
        //TODO okhttpd implementation
        return new SyncNetworkResponse();
    }
}
