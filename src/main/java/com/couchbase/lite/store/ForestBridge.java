/**
 * Created by Hideki Itakura on 10/20/2015.
 * Copyright (c) 2015 Couchbase, Inc All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.couchbase.lite.store;

import com.couchbase.cbforest.Document;
import com.couchbase.cbforest.ForestException;
import com.couchbase.lite.internal.RevisionInternal;

import java.util.ArrayList;
import java.util.List;

public class ForestBridge {
    public final static String TAG = ForestBridge.class.getSimpleName();

    /**
     * in CBLForestBridge.m
     * + (CBL_MutableRevision*) revisionObjectFromForestDoc: (VersionedDocument&)doc
     * revID: (NSString*)revID
     * withBody: (BOOL)withBody
     */
    public static RevisionInternal revisionObjectFromForestDoc(
            Document doc,
            String revID,
            boolean withBody)
            throws ForestException {
        RevisionInternal rev;
        String docID = doc.getDocID();
        if (revID != null) {
            if (!doc.selectRevID(revID, false))
                return null;
        } else {
            if (!doc.selectCurrentRev())
                return null;
            revID = doc.getSelectedRevID();
        }
        rev = new RevisionInternal(docID, revID, doc.selectedRevDeleted());
        rev.setSequence(doc.getSelectedSequence());
        if (withBody && !loadBodyOfRevisionObject(rev, doc))
            return null;
        return rev;
    }

    /**
     * in CBLForestBridge.m
     * + (BOOL) loadBodyOfRevisionObject: (CBL_MutableRevision*)rev
     * doc: (VersionedDocument&)doc
     */
    public static boolean loadBodyOfRevisionObject(
            RevisionInternal rev,
            Document doc)
            throws ForestException {
        if (!doc.selectRevID(rev.getRevID(), true))
            return false;
        byte[] json = doc.getSelectedBody();
        if (json == null)
            return false;
        rev.setSequence(doc.getSelectedSequence());
        rev.setJSON(json);
        return true;
    }

    /**
     * in CBLForestBridge.m
     * + (NSArray*) getCurrentRevisionIDs: (VersionedDocument&)doc
     *                     includeDeleted: (BOOL)includeDeleted
     */
    public static List<String> getCurrentRevisionIDs(Document doc, boolean includeDeleted)
            throws ForestException {
        List<String> currentRevIDs = new ArrayList<String>();
        do {
            if(includeDeleted || !doc.selectedRevDeleted())
                currentRevIDs.add(doc.getSelectedRevID());
        } while (doc.selectNextLeaf(includeDeleted, false));
        return currentRevIDs;
    }

    /**
     * in CBLForestBridge.m
     * + (NSArray*) getRevisionHistory: (const Revision*)revNode
     *
     * Note: Unable to downcast from RevTree to VersionedDocument
     * Instead of downcast, add docID parameter
     */
    public static List<RevisionInternal> getRevisionHistory(Document doc) {
        List<RevisionInternal> history = new ArrayList<RevisionInternal>();
        do{
            RevisionInternal rev = new RevisionInternal(
                    doc.getDocID(),
                    doc.getSelectedRevID(),
                    doc.selectedRevDeleted());
            rev.setMissing(doc.hasRevisionBody());
            history.add(rev);
        }while(doc.selectParentRev());
        return history;
    }
}