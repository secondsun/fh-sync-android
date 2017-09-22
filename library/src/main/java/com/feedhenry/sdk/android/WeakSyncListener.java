package com.feedhenry.sdk.android;

import android.util.Log;
import com.feedhenry.sdk.sync.FHSyncListener;
import com.feedhenry.sdk.sync.NotificationMessage;

import java.lang.ref.WeakReference;

import static com.feedhenry.sdk.sync.FHSyncUtils.notNullDo;

/**
 * Weak reference wrapper for sync listener.
 */
class WeakSyncListener implements FHSyncListener {

    private final WeakReference<FHSyncListener> ref;

    public WeakSyncListener(FHSyncListener listener) {
        ref = new WeakReference<>(listener);
    }

    public boolean hasLeaked() {
        return getWrapped() == null;
    }

    public FHSyncListener getWrapped() {
        return ref.get();
    }

    @Override
    public void onSyncStarted(final NotificationMessage message) {
        notNullDo(ref.get(), l -> onSyncStarted(message));
    }

    @Override
    public void onSyncCompleted(NotificationMessage message) {
        notNullDo(ref.get(), l -> onSyncCompleted(message));
    }

    @Override
    public void onUpdateOffline(NotificationMessage message) {
        notNullDo(ref.get(), l -> onUpdateOffline(message));
    }

    @Override
    public void onCollisionDetected(NotificationMessage message) {
        notNullDo(ref.get(), l -> onCollisionDetected(message));
    }

    @Override
    public void onRemoteUpdateFailed(NotificationMessage message) {
        notNullDo(ref.get(), l -> onRemoteUpdateFailed(message));
    }

    @Override
    public void onRemoteUpdateApplied(NotificationMessage message) {
        notNullDo(ref.get(), l -> onRemoteUpdateFailed(message));
    }

    @Override
    public void onLocalUpdateApplied(NotificationMessage message) {
        notNullDo(ref.get(), l -> onLocalUpdateApplied(message));
    }

    @Override
    public void onDeltaReceived(NotificationMessage message) {
        notNullDo(ref.get(), l -> onDeltaReceived(message));
    }

    @Override
    public void onSyncFailed(NotificationMessage message) {
        notNullDo(ref.get(), l -> onSyncFailed(message));
    }

    @Override
    public void onClientStorageFailed(NotificationMessage message) {
        notNullDo(ref.get(), l -> onClientStorageFailed(message));
    }

    @Override
    protected void finalize() throws Throwable {
        if (hasLeaked()) {
            Log.w("WeakSyncListener", "leaked");
        }
        super.finalize();
    }
}
