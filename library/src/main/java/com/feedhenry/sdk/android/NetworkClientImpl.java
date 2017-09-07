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
package com.feedhenry.sdk.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.feedhenry.sdk.exceptions.InvalidUrlException;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.network.SyncNetworkCallback;
import com.feedhenry.sdk.network.SyncNetworkResponse;
import com.feedhenry.sdk.utils.FHLog;
import com.squareup.okhttp.OkHttpClient;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.fh.JSONObject;

import java.io.IOException;

import static com.feedhenry.sdk.Sync.JSON;

public class NetworkClientImpl implements NetworkClient {
    private Context mContext;
    private boolean mIsOnline;
    private boolean mIsListenerRegistered;
    private NetworkReceiver mReceiver;

    private static final String LOG_TAG = "com.feedhenry.sdk.android.NetworkManager";
    private String cloudURL;
    private Headers headers;
    private okhttp3.OkHttpClient client;

    public NetworkClientImpl(Context pContext,String cloudURL) {
        this.mContext = pContext;
        client = new okhttp3.OkHttpClient.Builder().build();
        this.cloudURL=cloudURL;
    }

    public void registerNetworkListener() {
        if (!mIsListenerRegistered) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            mReceiver = new NetworkReceiver();
            mContext.registerReceiver(mReceiver, filter);
            mIsListenerRegistered = true;
            checkNetworkStatus();
        }
    }

    public void unregisterNetworkListener() {
        if (mIsListenerRegistered) {
            try {
                mContext.unregisterReceiver(mReceiver);
            } catch (Exception e) {
                FHLog.w(LOG_TAG, "Failed to unregister receiver");
            }
            mIsListenerRegistered = false;
        }
    }

    private void checkNetworkStatus() {
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        mIsOnline = networkInfo != null && networkInfo.isConnected();
        if (mIsOnline) {
            String type = networkInfo.getTypeName();
            FHLog.i(LOG_TAG, "Device is online. Connection type : " + type);
        } else {
            FHLog.i(LOG_TAG, "Device is offline.");
        }
    }

    @Override
    public void performRequest(String datasetName, JSONObject params, SyncNetworkCallback pCallback) {
        // TODO change params to json string.
        RequestBody body = RequestBody.create(JSON, params.toString());
        HttpUrl url = HttpUrl.parse(cloudURL);
        if (url != null) {
            url = url.newBuilder().addPathSegment(datasetName).build();
            Request.Builder builder = new Request.Builder();
            builder.url(url);
            if (headers != null) {
                builder.headers(headers);
            }
            Request request = builder.post(body).build();

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

    public boolean isOnline() {
        return mIsOnline;
    }

    /**
     * Configures HTTP headers that will be used to perform the cloud requests.
     *
     * @param requestHeaders list of HTTP headers
     */
    public void setHeaders(Headers requestHeaders) {
        headers = requestHeaders;
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkNetworkStatus();
        }
    }

}
