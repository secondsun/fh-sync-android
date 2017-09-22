package com.feedhenry.sdk.android;

import android.content.Context;
import com.feedhenry.sdk.android.utils.FHLog;
import com.feedhenry.sdk.network.NetworkClient;
import com.feedhenry.sdk.storage.Storage;
import com.feedhenry.sdk.utils.ClientIdGenerator;
import com.feedhenry.sdk.utils.Logger;
import com.feedhenry.sdk.utils.UtilFactory;

/**
 * Created on 9/20/17.
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
