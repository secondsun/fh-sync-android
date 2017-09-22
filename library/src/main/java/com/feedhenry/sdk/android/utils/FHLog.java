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
package com.feedhenry.sdk.android.utils;

import android.util.Log;
import com.feedhenry.sdk.utils.Logger;

public class FHLog implements Logger {

    private static FHLog instance;

    private int logLevel;

    private FHLog() {

    }

    public static FHLog getInstance() {
        if (instance == null) {
            instance = new FHLog();
        }
        return instance;
    }

    private void log(int pLogLevel, String pTag, String pMessage, Throwable pThrowable) {
        if (pLogLevel >= logLevel) {
            if (pLogLevel == LOG_LEVEL_VERBOSE) {
                Log.v(pTag, pMessage);
            } else if (pLogLevel == LOG_LEVEL_DEBUG) {
                Log.d(pTag, pMessage);
            } else if (pLogLevel == LOG_LEVEL_INFO) {
                Log.i(pTag, pMessage);
            } else if (pLogLevel == LOG_LEVEL_WARNING) {
                Log.w(pTag, pMessage);
            } else if (pLogLevel == LOG_LEVEL_ERROR) {
                if (null == pThrowable) {
                    Log.e(pTag, pMessage);
                } else {
                    Log.e(pTag, pMessage, pThrowable);
                }
            }
        }
    }

    @Override
    public void v(String pTag, String pMessage) {
        log(LOG_LEVEL_VERBOSE, pTag, pMessage, null);
    }

    @Override
    public void d(String pTag, String pMessage) {
        log(LOG_LEVEL_DEBUG, pTag, pMessage, null);
    }

    @Override
    public void i(String pTag, String pMessage) {
        log(LOG_LEVEL_INFO, pTag, pMessage, null);
    }

    @Override
    public void w(String pTag, String pMessage) {
        log(LOG_LEVEL_WARNING, pTag, pMessage, null);
    }

    @Override
    public void e(String pTag, String pMessage, Throwable pThrowable) {
        log(LOG_LEVEL_ERROR, pTag, pMessage, pThrowable);
    }

    /**
     * Sets the log level for the library.
     * The default level is {@link #LOG_LEVEL_ERROR}. Please make sure this is set to {@link #LOG_LEVEL_ERROR}
     * or {@link #LOG_LEVEL_NONE} before releasing the application.
     * The log level can be one of
     * <ul>
     * <li>{@link #LOG_LEVEL_VERBOSE}</li>
     * <li>{@link #LOG_LEVEL_DEBUG}</li>
     * <li>{@link #LOG_LEVEL_INFO}</li>
     * <li>{@link #LOG_LEVEL_WARNING}</li>
     * <li>{@link #LOG_LEVEL_ERROR}</li>
     * <li>{@link #LOG_LEVEL_NONE}</li>
     * </ul>
     *
     * @param logLevel The level of logging for the Sync library
     */
    @Override
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Gets the current log level for the Sync library.
     *
     * @return The current log level
     */
    public int getLogLevel() {
        return logLevel;
    }
}
