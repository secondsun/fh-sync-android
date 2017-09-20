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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.feedhenry.sdk.android.FileStorage;
import com.feedhenry.sdk.android.NetworkClientImpl;
import com.feedhenry.sdk.android.SyncableActivity;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.sync.FHSyncClient;
import com.feedhenry.sdk.sync.FHSyncConfig;
import okhttp3.Headers;
import okhttp3.MediaType;

/**
 * The Sync class provides static methods to initialize the library, create new instances of all the
 * API request objects, and configure global settings.
 */
public class Sync {

    private static final String TAG = "FHSync";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static final int LOG_LEVEL_VERBOSE = 1;
    public static final int LOG_LEVEL_DEBUG = 2;
    public static final int LOG_LEVEL_INFO = 3;
    public static final int LOG_LEVEL_WARNING = 4;
    public static final int LOG_LEVEL_ERROR = 5;
    public static final int LOG_LEVEL_NONE = Integer.MAX_VALUE;

    private static int mLogLevel = LOG_LEVEL_ERROR;

    private static String cloudURL;
    private static Headers mHeaders;

    private Sync() {
    }

    /**
     * Sync initialization.
     * @param application application object
     * @param config sync config
     * @param cloudUrl url of the cloud app endpoint
     */
    public static void init(final Application application, final FHSyncConfig config, final String cloudUrl) {
        cloudURL = cloudUrl;
        final NetworkClient networkClient = new NetworkClientImpl(application.getApplicationContext(),cloudUrl);
        networkClient.registerNetworkListener();
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (activity instanceof SyncableActivity) {
                    Log.d(TAG, "sync init for " + activity.getClass().getName());
                    Context context = application.getApplicationContext();
                    FHSyncClient.getInstance().init(config, new FileStorage(context),networkClient);
                    FHSyncClient.getInstance().setListener(((SyncableActivity) activity).onBindSyncListener());

                }
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (activity instanceof SyncableActivity) {
                    Log.d(TAG, "sync resumed for " + activity.getClass().getName());
                    FHSyncClient.getInstance().resumeSync(((SyncableActivity) activity).onBindSyncListener());
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (activity instanceof SyncableActivity) {
                    Log.d(TAG, "sync paused for " + activity.getClass().getName());
                    FHSyncClient.getInstance().pauseSync();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
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




}
