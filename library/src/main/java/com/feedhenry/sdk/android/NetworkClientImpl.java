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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.feedhenry.sdk.android.utils.FHLog;
import com.feedhenry.sdk.exceptions.InvalidUrlException;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.network.SyncNetworkCallback;
import com.feedhenry.sdk.network.SyncNetworkResponse;
import com.feedhenry.sdk.utils.ClientIdGenerator;
import com.feedhenry.sdk.utils.Logger;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class NetworkClientImpl implements NetworkClient {

    private static final String FH_CUID = "cuid";
    private static final String __FH = "__fh";
    private final ClientIdGenerator clientIdGenerator;
    private Context context;
    private boolean isOnline;
    private boolean isListenerRegistered;
    private NetworkReceiver receiver;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String LOG_TAG = "com.feedhenry.sdk.android.NetworkManager";
    private String cloudURL;
    private Headers headers;
    private static okhttp3.OkHttpClient client;
    private Logger log = FHLog.getInstance();

    public NetworkClientImpl(Context context, ClientIdGenerator clientIdGenerator) {
        this.context = context;
        if (client == null) {
            client = new okhttp3.OkHttpClient.Builder().build();
        }
        this.clientIdGenerator = clientIdGenerator;
    }

    public void registerNetworkListener() {
        if (!isListenerRegistered) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            receiver = new NetworkReceiver();
            context.registerReceiver(receiver, filter);
            isListenerRegistered = true;
            checkNetworkStatus();
        }
    }

    public void unregisterNetworkListener() {
        if (isListenerRegistered) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception e) {
                log.w(LOG_TAG, "Failed to unregister receiver");
            }
            isListenerRegistered = false;
        }
    }

    private void checkNetworkStatus() {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        isOnline = networkInfo != null && networkInfo.isConnected();
        if (isOnline) {
            String type = networkInfo.getTypeName();
            log.i(LOG_TAG, "Device is online. Connection type : " + type);
        } else {
            log.i(LOG_TAG, "Device is offline.");
        }
    }

    @Override
    public void performRequest(String datasetName, JSONObject params, SyncNetworkCallback callback) {

        try {
            params.put(__FH, new JSONObject().put(FH_CUID, clientIdGenerator.getClientId())); //adds client unique id

            RequestBody body = RequestBody.create(JSON, params.toString());
            if (cloudURL == null) {
                throw new IllegalStateException("Forgot to set cloud url!");
            }
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
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String result = responseBody.string();

                        SyncNetworkResponse syncNetworkResponse = new SyncNetworkResponse(new JSONObject(result));
                        callback.success(syncNetworkResponse);

                    } else {
                        callback.fail(new SyncNetworkResponse(null, "Null response."));
                    }
                } catch (IOException e) {
                    callback.fail(new SyncNetworkResponse(e, "Request failed"));
                }
            } else {
                callback.fail(new SyncNetworkResponse(new InvalidUrlException(cloudURL), "Invalid cloud app URL"));
            }
        } catch (JSONException e) {
            callback.fail(new SyncNetworkResponse(e, "JSON parsing failed."));
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    /**
     * Configures HTTP headers that will be used to perform the cloud requests.
     *
     * @param requestHeaders list of HTTP headers
     */
    public void setHeaders(Headers requestHeaders) {
        headers = requestHeaders;
    }

    @Override
    public void setCloudURL(String cloudURL) {
        this.cloudURL = cloudURL;
    }

    private class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            checkNetworkStatus();
        }
    }

}
