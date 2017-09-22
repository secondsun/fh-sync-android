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

import org.json.JSONException;
import org.json.JSONObject;

public class FHSyncDataRecord {

    private String hashValue;

    private JSONObject data;

    private String uid;

    private static final String KEY_HASH = "hashValue";

    private static final String KEY_DATA = "data";

    private static final String KEY_UID = "uid";

    public FHSyncDataRecord() {

    }

    public FHSyncDataRecord(JSONObject pData) throws JSONException {
        setData(pData);
    }

    public FHSyncDataRecord(String pUid, JSONObject pData) throws JSONException {
        this.uid = pUid;
        setData(pData);
    }

    public String getHashValue() {
        return hashValue;
    }

    public JSONObject getData() {
        return data;
    }

    public String getUid() {
        return uid;
    }

    public void setData(JSONObject pData) throws JSONException {
        data = new JSONObject(pData.toString());
        hashValue = FHSyncUtils.generateObjectHash(data);
    }

    public void setUid(String pUid) {
        this.uid = pUid;
    }

    public void setHashValue(String pHashValue) {
        this.hashValue = pHashValue;
    }

    public JSONObject getJSON() throws JSONException {
        JSONObject ret = new JSONObject();
        if (this.uid != null) {
            ret.put(KEY_UID, this.uid);
        }
        if (this.data != null) {
            ret.put(KEY_DATA, this.data);
        }
        if (this.hashValue != null) {
            ret.put(KEY_HASH, this.hashValue);
        }
        return ret;
    }

    public String toString() {
        try {
            return this.getJSON().toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean equals(Object pThat) {
        if (this == pThat) {
            return true;
        }

        if (pThat instanceof FHSyncDataRecord) {
            FHSyncDataRecord that = (FHSyncDataRecord) pThat;
            return this.getHashValue().equals(that.getHashValue());
        }
        return false;
    }

    public FHSyncDataRecord clone() {
        JSONObject jsonObj;
        try {
            jsonObj = this.getJSON();
            return FHSyncDataRecord.fromJSON(jsonObj);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static FHSyncDataRecord fromJSON(JSONObject pObj) throws JSONException {
        FHSyncDataRecord record = new FHSyncDataRecord();
        if (pObj.has(KEY_UID)) {
            record.setUid(pObj.getString(KEY_UID));
        }
        if (pObj.has(KEY_DATA)) {
            record.setData(pObj.getJSONObject(KEY_DATA));
        }
        if (pObj.has(KEY_HASH)) {
            record.setHashValue(pObj.getString(KEY_HASH));
        }
        return record;
    }
}
