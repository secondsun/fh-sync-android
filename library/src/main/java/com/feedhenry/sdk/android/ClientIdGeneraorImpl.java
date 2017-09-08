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
package com.feedhenry.sdk.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import com.feedhenry.sdk.utils.ClientIdGenerator;

/**
 * Generates unique client id on Android.
 */
public class ClientIdGeneraorImpl implements ClientIdGenerator {

    private static final String FH_SYNC_PREFERENCES = "fh-sync";
    private static final String KEY_CLIENT_ID = "clientId";

    private final Context context;

    public ClientIdGeneraorImpl(Context ctx) {
        context = ctx;
    }

    @Override
    public String getClientId() {
        SharedPreferences prefs = context.getSharedPreferences(FH_SYNC_PREFERENCES, Context.MODE_PRIVATE);
        String clientId = prefs.getString(KEY_CLIENT_ID, generateNewId());
        prefs.edit().putString(KEY_CLIENT_ID, clientId).apply();
        return clientId;
    }

    @SuppressLint("HardwareIds")
    private String generateNewId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
