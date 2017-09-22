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

import android.content.Context;
import com.feedhenry.sdk.android.utils.FHLog;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.storage.Storage;
import com.feedhenry.sdk.utils.ClientIdGenerator;
import com.feedhenry.sdk.utils.Logger;
import com.feedhenry.sdk.utils.UtilFactory;

/**
 * Factory provides Android specific implementations.
 */

public class AndroidUtilFactory implements UtilFactory {

    private final Context context;
    private ClientIdGenerator clientIdGenerator;
    private NetworkClientImpl networkClient;
    private Storage storage;

    public AndroidUtilFactory(Context ctx) {
        context = ctx;
    }

    @Override
    public Logger getLogger() {
        return FHLog.getInstance();
    }

    @Override
    public ClientIdGenerator getClientIdGenerator() {
        if (clientIdGenerator == null) {
            clientIdGenerator = new ClientIdGeneratorImpl(context);
        }
        return clientIdGenerator;
    }

    @Override
    public NetworkClient getNetworkClient() {
        if (networkClient == null) {
            networkClient = new NetworkClientImpl(context, getClientIdGenerator());
        }
        return networkClient;
    }

    @Override
    public Storage getStorage() {
        if (storage == null) {
            storage = new FileStorage(context);
        }
        return storage;
    }
}
