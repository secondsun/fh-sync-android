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
import android.support.annotation.NonNull;
import com.feedhenry.sdk.storage.Storage;

import java.io.*;
import java.lang.ref.WeakReference;

/**
 * Storage in Android local data store using files.
 */
public class FileStorage implements Storage {

    private static final String STORAGE_FILE_EXT = ".sync.json";
    private static final int BUFFER_SIZE = 1024;
    private final WeakReference<Context> weakContext;

    public FileStorage(Context ctx) {
        weakContext = new WeakReference<>(ctx);
    }

    private static void writeStream(InputStream input, OutputStream output) throws IOException {
        if (input != null && output != null) {
            BufferedInputStream bis = new BufferedInputStream(input);
            BufferedOutputStream bos = new BufferedOutputStream(output);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bos.close();
            bis.close();
        }
    }

    private String getFilePath(@NonNull String contentId) {
        return contentId + STORAGE_FILE_EXT;
    }

    @Override
    public byte[] getContent(@NonNull String contentId) throws IOException {
        String filePath = getFilePath(contentId);
        Context ctx = weakContext.get();
        if (ctx != null) {
            FileInputStream fis = ctx.openFileInput(filePath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileStorage.writeStream(fis, bos);
            return bos.toByteArray();
        } else {
            throw new IllegalStateException("Con't open file when Context is destroyed");
        }
    }

    @Override
    public void putContent(@NonNull String contentId, @NonNull byte[] content) throws IOException {
        String filePath = getFilePath(contentId);
        Context ctx = weakContext.get();
        if (ctx != null) {
            FileOutputStream fos = ctx.openFileOutput(filePath, Context.MODE_PRIVATE);
            ByteArrayInputStream bis = new ByteArrayInputStream(content);
            FileStorage.writeStream(bis, fos);
        } else {
            throw new IllegalStateException("Con't open file when Context is destroyed");
        }
    }

}
