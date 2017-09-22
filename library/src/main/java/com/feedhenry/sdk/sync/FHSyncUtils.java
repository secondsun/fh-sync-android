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

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FHSyncUtils {

    public interface Action1<T> {

        void doAction(T obj);
    }

    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private static final String TAG = "FHSyncUtils";

    public static JSONArray sortObj(JSONArray object) throws JSONException {
        JSONArray results = new JSONArray();
        for (int i = 0, length = object.length(); i < length; i++) {
            JSONObject obj = new JSONObject();
            obj.put("key", i + "");
            Object value = object.get(i);
            if (value instanceof JSONObject || value instanceof JSONArray) {
                obj.put("value", sortObj(value));
            } else {
                obj.put("value", value);
            }
            results.put(obj);
        }

        return results;
    }

    public static JSONArray sortObj(JSONObject object) throws JSONException {
        JSONArray results = new JSONArray();
        JSONArray keys = object.names();
        List<String> sortedKeys = sortNames(keys);
        for (String sortedKey : sortedKeys) {
            JSONObject obj = new JSONObject();
            Object value = object.get(sortedKey);
            obj.put("key", sortedKey);
            if (value instanceof JSONObject || value instanceof JSONArray) {
                obj.put("value", sortObj(value));
            } else {
                obj.put("value", value);
            }
            results.put(obj);
        }

        return results;
    }

    public static String generateObjectHash(JSONArray object) throws JSONException {
        JSONArray sorted = sortObj(object);
        String hashValue = generateHash(sorted.toString());

        return hashValue;
    }

    public static String generateObjectHash(JSONObject object) throws JSONException {
        String hashValue = "";
        JSONArray sorted = sortObj(object);
        hashValue = generateHash(sorted.toString());

        return hashValue;
    }

    public static String generateHash(String text) {
        try {
            String hashValue;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.reset();
            md.update(text.getBytes("ASCII"));
            hashValue = encodeHex(md.digest());
            return hashValue;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private static String encodeHex(byte[] data) {
        int l = data.length;

        char[] out = new char[l << 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }

        return new String(out);
    }

    private static Object sortObj(Object value) throws JSONException {
        if (value instanceof JSONArray) {
            return sortObj((JSONArray) value);
        } else if (value instanceof JSONObject) {
            return sortObj((JSONObject) value);
        } else {
            throw new IllegalArgumentException(String.format("A object %s was snuck into a JSON tree", value.toString()));
        }
    }

    private static List<String> sortNames(JSONArray names) throws JSONException {
        if (names == null) {
            return Collections.emptyList();
        }

        int length = names.length();
        ArrayList<String> sortedNames = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            sortedNames.add(names.getString(i));
        }
        Collections.sort(sortedNames);
        return sortedNames;
    }

    /**
     * Executes action only if obj is not null
     *
     * @param obj    object to be tested for null
     * @param action action where object is passed when not null
     */
    public static <T> void notNullDo(T obj, Action1<T> action) {
        if (obj != null) {
            action.doAction(obj);
        }
    }

}
