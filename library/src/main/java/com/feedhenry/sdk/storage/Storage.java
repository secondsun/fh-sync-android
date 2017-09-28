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

import java.io.IOException;

/**
 * Interface for classes that should be used for retrieving and saving data for datasets.
 */
public interface Storage {

    /**
     * Opens file for reading
     *
     * @param contentId content identifier
     *
     * @return input stream
     */
    byte[] getContent(@NonNull String contentId) throws IOException;

    /**
     * Writes content to a file
     *
     * @param contentId relative path to file
     * @param content content to write
     *
     */
    void putContent(@NonNull String contentId, @NonNull byte[] content) throws IOException;
}
