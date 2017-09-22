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
package com.feedhenry.sdk.sync;

import android.support.test.runner.AndroidJUnit4;
import com.feedhenry.sdk.android.AndroidUtilFactory;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getContext;
import static com.feedhenry.sdk.utils.Logger.LOG_LEVEL_VERBOSE;

@RunWith(AndroidJUnit4.class)
public class FHSyncClientTest {

    private MockWebServer mockWebServer = null;
    private FHSyncClient client;
    private AndroidUtilFactory utilFactory;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(9000);
        utilFactory = new AndroidUtilFactory(getContext());
        utilFactory.getLogger().setLogLevel(LOG_LEVEL_VERBOSE);
    }

    @After
    public void tearDown() throws Exception {
        try {
            mockWebServer.shutdown();
            Thread.sleep(100);
        } catch (IOException | InterruptedException | AssertionError ignore) {

        }
    }

    @Test
    public void testInitDestroyInitDestroyDoesNotThrowNPE() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        client = new FHSyncClient(new FHSyncConfig(), utilFactory);
        client.setListener(new LockingSyncListener(latch));
        client.stopSync(false);
        client.destroy();

        latch = new CountDownLatch(1);
        client = new FHSyncClient(new FHSyncConfig(), utilFactory);
        client.setListener(new LockingSyncListener(latch));
        client.stopSync(false);
        client.destroy();

    }

    private static class LockingSyncListener implements FHSyncListener {

        final CountDownLatch latch;

        public LockingSyncListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSyncStarted(NotificationMessage message) {
            latch.countDown();
        }

        @Override
        public void onSyncCompleted(NotificationMessage message) {

        }

        @Override
        public void onUpdateOffline(NotificationMessage message) {

        }

        @Override
        public void onCollisionDetected(NotificationMessage message) {

        }

        @Override
        public void onRemoteUpdateFailed(NotificationMessage message) {

        }

        @Override
        public void onRemoteUpdateApplied(NotificationMessage message) {

        }

        @Override
        public void onLocalUpdateApplied(NotificationMessage message) {

        }

        @Override
        public void onDeltaReceived(NotificationMessage message) {

        }

        @Override
        public void onSyncFailed(NotificationMessage message) {

        }

        @Override
        public void onClientStorageFailed(NotificationMessage message) {

        }
    }

}
