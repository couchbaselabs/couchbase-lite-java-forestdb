/**
 * Created by Hideki Itakura on 10/20/2015.
 * Copyright (c) 2015 Couchbase, Inc All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.couchbase.lite.store;

import com.couchbase.cbforest.Constants;
import com.couchbase.cbforest.Document;
import com.couchbase.cbforest.ForestException;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Status;
import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForestBridge implements Constants {
    public final static String TAG = ForestBridge.class.getSimpleName();

    /**
     * in CBLForestBridge.m
     * + (CBL_MutableRevision*) revisionObjectFromForestDoc: (VersionedDocument&)doc
     * revID: (NSString*)revID
     * withBody: (BOOL)withBody
     */
    public static RevisionInternal revisionObject(
            Document doc,
            String docID,
            String revID,
            boolean withBody) {

        boolean deleted = doc.selectedRevDeleted();
        if (revID == null)
            revID = doc.getSelectedRevID();
        RevisionInternal rev = new RevisionInternal(docID, revID, deleted);
        rev.setSequence(doc.getSelectedSequence());
        if (withBody) {
            Status status = loadBodyOfRevisionObject(rev, doc);
            if (status.isError() && status.getCode() != Status.GONE)
                return null;
        }
        return rev;
    }

    /**
     * in CBLForestBridge.m
     * + (BOOL) loadBodyOfRevisionObject: (CBL_MutableRevision*)rev
     * doc: (VersionedDocument&)doc
     */
    public static Status loadBodyOfRevisionObject(RevisionInternal rev, Document doc) {
        try {
            rev.setSequence(doc.getSelectedSequence());
            doc.selectRevID(rev.getRevID(), true);
            rev.setJSON(doc.getSelectedBody());
            return new Status(Status.OK);
        } catch (ForestException ex) {
            rev.setMissing(true);
            return err2status(ex);
        }
    }

    /**
     * in CBLForestBridge.m
     * + (BOOL) loadBodyOfRevisionObject: (CBL_MutableRevision*)rev
     * doc: (VersionedDocument&)doc
     */
    public static Map<String, Object> bodyOfSelectedRevision(Document doc) {
        byte[] body;
        try {
            body = doc.getSelectedBody();
        } catch (ForestException e) {
            return null;
        }
        Map<String, Object> properties = null;
        if (body != null && body.length > 0) {
            try {
                properties = Manager.getObjectMapper().readValue(body, Map.class);
            } catch (IOException e) {
                Log.w(TAG, "Failed to parse body: [%s]", new String(body));
            }
        }
        return properties;
    }


    /**
     * Not include deleted leaf node
     */
    public static List<String> getCurrentRevisionIDs(Document doc) throws ForestException {
        List<String> currentRevIDs = new ArrayList<String>();
        do {
            currentRevIDs.add(doc.getSelectedRevID());
        } while (doc.selectNextLeaf(false, false));
        return currentRevIDs;
    }

    /**
     * in CBLForestBridge.m
     * CBLStatus err2status(C4Error c4err)
     */
    public static Status err2status(ForestException ex) {
        return new Status(_err2status(ex));
    }

    /**
     * in CBLForestBridge.m
     * CBLStatus err2status(C4Error c4err)
     */
    public static int _err2status(ForestException ex) {
        if (ex == null || ex.code == 0)
            return Status.OK;

        Log.d(TAG, "[_err2status()] ForestException: domain=%d, code=%d", ex, ex.domain, ex.code);

        switch (ex.domain) {
            case C4ErrorDomain.HTTPDomain:
                return ex.code;
            case C4ErrorDomain.POSIXDomain:
                break;
            case C4ErrorDomain.ForestDBDomain:
                switch (ex.code) {
                    case FDBErrors.FDB_RESULT_SUCCESS:
                        return Status.OK;
                    case FDBErrors.FDB_RESULT_KEY_NOT_FOUND:
                    case FDBErrors.FDB_RESULT_NO_SUCH_FILE:
                        return Status.NOT_FOUND;
                    case FDBErrors.FDB_RESULT_RONLY_VIOLATION:
                        return Status.FORBIDDEN;
                    case FDBErrors.FDB_RESULT_NO_DB_HEADERS:
                    case FDBErrors.FDB_RESULT_CRYPTO_ERROR:
                        return Status.UNAUTHORIZED;
                    case FDBErrors.FDB_RESULT_CHECKSUM_ERROR:
                    case FDBErrors.FDB_RESULT_FILE_CORRUPTION:
                        return Status.CORRUPT_ERROR;
                }
                break;
            case C4ErrorDomain.C4Domain:
                switch (ex.code) {
                    case C4DomainErrorCode.kC4ErrorCorruptRevisionData:
                    case C4DomainErrorCode.kC4ErrorCorruptIndexData:
                        return Status.CORRUPT_ERROR;
                    case C4DomainErrorCode.kC4ErrorBadRevisionID:
                        return Status.BAD_ID;
                    case C4DomainErrorCode.kC4ErrorAssertionFailed:
                        break;
                    default:
                        break;
                }
                break;
        }
        return Status.DB_ERROR;
    }
}
