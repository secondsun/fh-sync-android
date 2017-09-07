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
package com.feedhenry.sdk.storage;

import android.support.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Interface for classes that should handle opening files.
 */
public interface FileStorage {

    /**
     * Opens file for reading
     *
     * @param filePath relative path to file
     *
     * @return input stream
     */
    FileInputStream openFileInput(@NonNull String filePath) throws FileNotFoundException;

    /**
     * Opens file for writing
     *
     * @param filePath relative path to file
     *
     * @return output stream
     */
    FileOutputStream openFileOutput(@NonNull String filePath) throws FileNotFoundException;
}
