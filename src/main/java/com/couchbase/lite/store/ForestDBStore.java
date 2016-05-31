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
import com.couchbase.cbforest.Database;
import com.couchbase.cbforest.Document;
import com.couchbase.cbforest.DocumentIterator;
import com.couchbase.cbforest.ForestException;
import com.couchbase.lite.BlobKey;
import com.couchbase.lite.ChangesOptions;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Misc;
import com.couchbase.lite.Predicate;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryOptions;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.ReplicationFilter;
import com.couchbase.lite.Revision;
import com.couchbase.lite.RevisionList;
import com.couchbase.lite.Status;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.View;
import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.support.RevisionUtils;
import com.couchbase.lite.support.action.Action;
import com.couchbase.lite.support.action.ActionBlock;
import com.couchbase.lite.support.action.ActionException;
import com.couchbase.lite.support.security.SymmetricKey;
import com.couchbase.lite.util.Log;
import com.couchbase.lite.util.NativeLibUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForestDBStore implements Store, EncryptableStore, Constants {

    public static String TAG = Log.TAG_DATABASE;

    private final static String NATIVE_LIB_NAME = "CouchbaseLiteJavaForestDB";

    /** static constructor */
    static {
        try {
            System.loadLibrary(NATIVE_LIB_NAME);
        } catch (UnsatisfiedLinkError e) {
            if (!NativeLibUtils.loadLibrary(NATIVE_LIB_NAME))
                Log.e(TAG, "ERROR: Failed to load %s", NATIVE_LIB_NAME);
        }
    }

    public static String kDBFilename = "db.forest";

    private static final int kDefaultMaxRevTreeDepth = 20;

    protected String directory;
    private String forestPath;
    private Manager manager;
    protected Database forest;
    private StoreDelegate delegate;
    private int maxRevTreeDepth;
    private boolean autoCompact;
    private boolean readOnly = false;
    private SymmetricKey encryptionKey;

    private ThreadLocal<Integer> transactionLevel4Thread = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    // Native method for deriving PBDDF2-SHA256 key:
    private static native byte[] nativeDerivePBKDF2SHA256Key(
            String password, byte[] salt, int rounds);

    ///////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////

    public ForestDBStore(String directory, Manager manager, StoreDelegate delegate) {
        assert (new File(directory).isAbsolute()); // path must be absolute
        this.directory = directory;
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("directory '%s' does not exist or not directory", directory));
        }
        this.forestPath = new File(directory, kDBFilename).getPath();
        this.manager = manager;
        this.delegate = delegate;

        this.forest = null;
        this.autoCompact = true;
        this.maxRevTreeDepth = kDefaultMaxRevTreeDepth;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of Storage
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // INITIALIZATION AND CONFIGURATION:
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean databaseExists(String directory) {
        if (new File(directory, kDBFilename).exists())
            return true;
        // If "db.forest" doesn't exist (auto-compaction will add numeric suffixes), check for meta:
        return new File(directory, kDBFilename + ".meta").exists();
    }

    @Override
    public void open() throws CouchbaseLiteException {
        // Flag:
        int flags = readOnly ? Database.ReadOnly : Database.Create;
        if(autoCompact)
            flags |= Database.AutoCompact;

        // Encryption:
        int enAlgorithm = Database.NoEncryption;
        byte[] enKey = null;
        if (encryptionKey != null) {
            enAlgorithm = Database.AES256Encryption;
            enKey = encryptionKey.getKey();
        }

        try {
            forest = new Database(forestPath, flags, enAlgorithm, enKey);
        } catch (ForestException e) {
            Log.e(TAG, "Failed to open the forestdb: domain=%d, error=%d", e.domain, e.code, e);
            if (e.domain == C4ErrorDomain.ForestDBDomain &&
                    (e.code == FDBErrors.FDB_RESULT_NO_DB_HEADERS ||
                            e.code == FDBErrors.FDB_RESULT_CRYPTO_ERROR)) {
                throw new CouchbaseLiteException("Cannot create database", e, Status.UNAUTHORIZED);
            }
            throw new CouchbaseLiteException("Cannot create database", e, Status.DB_ERROR);
        }
    }

    @Override
    public void close() {
        if (forest != null) {
            try {
                forest.close();
            } catch (ForestException e) {
                Log.e(TAG, "Failed to close Database: " + forest);
            }
            forest = null;
        }
    }

    @Override
    public void setDelegate(StoreDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public StoreDelegate getDelegate() {
        return delegate;
    }

    @Override
    public void setMaxRevTreeDepth(int maxRevTreeDepth) {
        this.maxRevTreeDepth = maxRevTreeDepth;
    }

    @Override
    public int getMaxRevTreeDepth() {
        return maxRevTreeDepth;
    }

    @Override
    public void setAutoCompact(boolean value) {
        autoCompact = value;
    }

    @Override
    public boolean getAutoCompact() {
        return autoCompact;
    }

    ///////////////////////////////////////////////////////////////////////////
    // DATABASE ATTRIBUTES & OPERATIONS:
    ///////////////////////////////////////////////////////////////////////////

    // #pragma mark - INFO FOR KEY:

    /**
     * TODO return value from long to Status
     */
    @Override
    public long setInfo(String key, String info) {
        final String k = key;
        final String i = info;
        try {
            Status status = inTransaction(new Task() {
                @Override
                public Status run() {
                    try {
                        forest.rawPut("info", k, null, i == null ? null : i.getBytes());
                        return new Status(Status.OK);
                    } catch (ForestException e) {
                        Log.e(TAG, "Error in KeyStoreWriter.set()", e);
                        return ForestBridge.err2status(e);
                    }
                }
            });
            return status.getCode();
        } catch (Exception e) {
            Log.e(TAG, "Exception in setInfo()", e);
            return Status.UNKNOWN;
        }
    }

    @Override
    public String getInfo(String key) {
        try {
            byte[][] metaNbody = forest.rawGet("info", key);
            return new String(metaNbody[1]);
        } catch (ForestException e) {
            // KEY NOT FOUND
            if (e.domain == C4ErrorDomain.ForestDBDomain &&
                    e.code == FDBErrors.FDB_RESULT_KEY_NOT_FOUND) {
                Log.i(TAG, "[getInfo()] Key(\"%s\") is not found.", key);
            }
            // UNEXPECTED ERROR
            else {
                Log.e(TAG, "[getInfo()] Unexpected Error", e);
            }
            return null;
        }
    }

    @Override
    public int getDocumentCount() {
        return (int) forest.getDocumentCount();
    }

    @Override
    public long getLastSequence() {
        return forest.getLastSequence();
    }

    @Override
    public boolean inTransaction() {
        return transactionLevel4Thread.get() > 0;
    }

    @Override
    public void compact() throws CouchbaseLiteException {
        try {
            forest.compact();
        } catch (ForestException e) {
            Log.e(TAG, "Failed to compact(): domain=%d code=%d", e, e.domain, e.code);
            throw new CouchbaseLiteException(Status.UNKNOWN);
        }
    }

    @Override
    public boolean runInTransaction(TransactionalTask task) {
        if (inTransaction())
            return task.run();
        else {
            boolean commit = true;
            beginTransaction();
            try {
                commit = task.run();
            } catch (Exception e) {
                commit = false;
                Log.e(TAG, e.toString(), e);
                throw new RuntimeException(e);
            } finally {
                endTransaction(commit);
            }
            return commit;
        }
    }

    @Override
    public RevisionInternal getDocument(String docID, String revID, boolean withBody) {
        Document doc = getDocument(docID);
        if (doc == null)
            return null;
        try {
            Status status = selectRev(doc, revID, withBody);
            if (status.isError())
                return null;
            if (revID == null && doc.selectedRevDeleted())
                return null;
            return ForestBridge.revisionObject(doc, docID, revID, withBody);
        } finally {
            doc.free();
        }
    }

    private Document getDocument(String docID) {
        try {
            return _getDocument(docID);
        } catch (CouchbaseLiteException e) {
            return null;
        }
    }

    /**
     * @note return value should not be null.
     */
    private Document _getDocument(String docID) throws CouchbaseLiteException {
        Document doc;
        try {
            doc = forest.getDocument(docID, true);
        } catch (ForestException e) {
            Log.w(TAG, "ForestDB Error: getDocument(docID, true) docID=[%s] error=[%s]",
                    docID, e.toString());
            throw new CouchbaseLiteException(ForestBridge.err2status(e));
        }
        if (!doc.exists()) {
            doc.free();
            throw new CouchbaseLiteException(Status.NOT_FOUND);
        }
        return doc;
    }

    private Status selectRev(Document doc, String revID, boolean withBody) {
        Status status = new Status(Status.OK);
        if (revID != null) {
            try {
                doc.selectRevID(revID, withBody);
            } catch (ForestException e) {
                status = ForestBridge.err2status(e);
            }
        } else {
            if (!doc.selectCurrentRev())
                status = new Status(Status.DELETED);
        }
        return status;
    }

    @Override
    public RevisionInternal loadRevisionBody(RevisionInternal rev)
            throws CouchbaseLiteException {
        Document doc = _getDocument(rev.getDocID());
        Status status = selectRev(doc, rev.getRevID(), true);
        if (status.isError())
            throw new CouchbaseLiteException(status);
        status = ForestBridge.loadBodyOfRevisionObject(rev, doc);
        if (status.isError())
            throw new CouchbaseLiteException(status);
        return rev;
    }

    @Override
    public RevisionInternal getParentRevision(RevisionInternal rev) {
        if (rev.getDocID() == null || rev.getRevID() == null)
            return null;
        Document doc = getDocument(rev.getDocID());
        if (doc == null)
            return null;
        try {
            Status status = selectRev(doc, rev.getRevID(), true);
            if (status.isError())
                return null;
            if (!doc.selectParentRev())
                return null;
            return ForestBridge.revisionObject(doc, rev.getDocID(), null, true);
        } finally {
            doc.free();
        }
    }

    // TODO: Set<String> ancestorRevIDs as additional parameter
    @Override
    public List<RevisionInternal> getRevisionHistory(RevisionInternal rev) {
        Document doc = getDocument(rev.getDocID());
        if (doc == null)
            return null;
        try {
            try {
                if (!doc.selectRevID(rev.getRevID(), false))
                    return null;
            } catch (ForestException e) {
                Log.e(TAG, "Error in getRevisionHistory() rev=" + rev, e);
                return null;
            }
            List<RevisionInternal> history = new ArrayList<RevisionInternal>();
            do {
                RevisionInternal ancestor = ForestBridge.revisionObject(
                        doc, rev.getDocID(), null, false);
                if (ancestor == null)
                    break;
                ancestor.setMissing(doc.hasRevisionBody());
                history.add(ancestor);
                // TODO
                //if(ancestorRevIDs!=null&&ancestorRevIDs.contains(ancestor.getRevID()))
                //    break;
            } while (doc.selectParentRev());
            return history;
        } finally {
            doc.free();
        }
    }

    @Override
    public RevisionList getAllRevisions(String docID, boolean onlyCurrent) {
        Document doc = getDocument(docID);
        if (doc == null)
            return null;
        try {
            RevisionList revs = new RevisionList();
            do {
                if (onlyCurrent && !doc.selectedRevLeaf())
                    continue;
                RevisionInternal rev = ForestBridge.revisionObject(doc, docID, null, false);
                if (rev != null)
                    revs.add(rev);
            } while (doc.selectNextRev());
            return revs;
        } finally {
            doc.free();
        }
    }

    @Override
    public List<String> getPossibleAncestorRevisionIDs(RevisionInternal rev,
                                                       int limit,
                                                       AtomicBoolean onlyAttachments) {
        int generation = RevisionInternal.generationFromRevID(rev.getRevID());
        if (generation <= 1)
            return null;

        Document doc = getDocument(rev.getDocID());
        if (doc == null)
            return null;

        try {
            List<String> revIDs = new ArrayList<String>();
            do {
                String revID = doc.getSelectedRevID();
                if (RevisionInternal.generationFromRevID(revID) < generation
                        && !doc.selectedRevDeleted()
                        && doc.hasRevisionBody()
                        && !(onlyAttachments.get() && !doc.selectedRevHasAttachments())) {
                    if (onlyAttachments != null && revIDs.size() == 0) {
                        onlyAttachments.set(doc.selectedRevHasAttachments());
                    }
                    revIDs.add(revID);
                    if (limit > 0 && revIDs.size() >= limit)
                        break;
                }
            } while (doc.selectNextRev());
            return revIDs;
        } finally {
            doc.free();
        }
    }

    @Override
    public int findMissingRevisions(RevisionList revs) {
        int numRevisionsRemoved = 0;
        if (revs.size() == 0)
            return numRevisionsRemoved;

        RevisionList sortedRevs = (RevisionList) revs.clone();
        sortedRevs.sortByDocID();

        Document doc = null;
        String lastDocID = null;
        for (int i = 0; i < sortedRevs.size(); i++) {
            RevisionInternal rev = sortedRevs.get(i);
            if (!rev.getDocID().equals(lastDocID)) {
                lastDocID = rev.getDocID();
                if (doc != null)
                    doc.free();
                try {
                    doc = forest.getDocument(rev.getDocID(), true);
                } catch (ForestException e) {
                    Status status = ForestBridge.err2status(e);
                    if (status.getCode() != Status.NOT_FOUND)
                        Log.e(TAG, "Error in getDocument() docID=" + rev.getDocID(), e);
                    doc = null;
                }
            }
            try {
                if (doc != null && doc.selectRevID(rev.getRevID(), false)) {
                    revs.remove(rev); // not missing, so remove from list
                    numRevisionsRemoved += 1;
                }
            } catch (ForestException e) {
                // ignore
            }
        }
        if (doc != null)
            doc.free();
        return numRevisionsRemoved;
    }

    @Override
    public String findCommonAncestorOf(RevisionInternal rev, List<String> revIDs) {
        long generation = Revision.generationFromRevID(rev.getRevID());
        if (generation <= 1 || (revIDs == null || revIDs.size() == 0))
            return null;
        Collections.sort(revIDs, new Comparator<String>() {
            @Override
            public int compare(String id1, String id2) {
                // descending order of generation
                return RevisionInternal.CBLCompareRevIDs(id2, id1);
            }
        });
        Document doc = getDocument(rev.getDocID());
        if (doc == null)
            return null;
        String commonAncestor = null;
        try {
            for (String possibleRevID : revIDs) {
                if (Revision.generationFromRevID(possibleRevID) <= generation) {
                    try {
                        if (doc.selectRevID(possibleRevID, false))
                            commonAncestor = possibleRevID;
                    } catch (ForestException e) {
                        Log.i(TAG, "Error in Document.selectRevID() revID=%s", e, possibleRevID);
                    }
                    if (commonAncestor != null)
                        break;
                }
            }
        } finally {
            doc.free();
        }
        return commonAncestor;
    }

    @Override
    public Set<BlobKey> findAllAttachmentKeys() throws CouchbaseLiteException {
        Set<BlobKey> keys = new HashSet<BlobKey>();
        try {
            DocumentIterator itr = forest.iterator(null, null, 0, IteratorFlags.kDefault);
            Document doc;
            while ((doc = itr.nextDocument()) != null) {
                try {
                    if (!doc.hasAttachments() || (doc.deleted() && !doc.conflicted()))
                        continue;
                    // Since db is assumed to have just been compacted,
                    // we know that non-current revisions
                    // won't have any bodies. So only scan the current revs.
                    do {
                        if (doc.selectedRevHasAttachments()) {
                            byte[] body = doc.getSelectedBody();
                            if (body != null && body.length > 0) {
                                Map<String, Object> props = getDocProperties(body);
                                if (props != null && props.containsKey("_attachments")) {
                                    Map<String, Object> attachments =
                                            (Map<String, Object>) props.get("_attachments");
                                    Iterator<String> itr2 = attachments.keySet().iterator();
                                    while (itr2.hasNext()) {
                                        String name = itr2.next();
                                        Map<String, Object> attachment =
                                                (Map<String, Object>) attachments.get(name);
                                        if (attachment != null &&
                                                attachment.containsKey("digest")) {
                                            String digest = (String) attachment.get("digest");
                                            if (digest != null) {
                                                keys.add(new BlobKey(digest));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } while (doc.selectNextLeaf(false, false));
                } finally {
                    doc.free();
                }
            }
        } catch (ForestException e) {
            throw new CouchbaseLiteException(ForestBridge.err2status(e));
        }
        return keys;
    }

    @Override
    public Map<String, Object> getAllDocs(QueryOptions options) throws CouchbaseLiteException {
        Map<String, Object> result = new HashMap<String, Object>();
        List<QueryRow> rows = new ArrayList<QueryRow>();

        if (options == null)
            options = new QueryOptions();

        boolean includeDocs = (options.isIncludeDocs() ||
                options.getPostFilter() != null ||
                options.getAllDocsMode() == Query.AllDocsMode.SHOW_CONFLICTS);
        boolean includeDeletedDocs = options.getAllDocsMode() == Query.AllDocsMode.INCLUDE_DELETED;
        int limit = options.getLimit();
        int skip = options.getSkip();
        Predicate<QueryRow> filter = options.getPostFilter();

        int iteratorFlags = IteratorFlags.kDefault;
        if (!includeDocs)
            iteratorFlags &= ~IteratorFlags.kIncludeBodies;
        if (options.isDescending())
            iteratorFlags |= IteratorFlags.kDescending;
        if (!options.isInclusiveStart())
            iteratorFlags &= ~IteratorFlags.kInclusiveStart;
        if (!options.isInclusiveEnd())
            iteratorFlags &= ~IteratorFlags.kInclusiveEnd;
        if (includeDeletedDocs)
            iteratorFlags |= IteratorFlags.kIncludeDeleted;
        // TODO: kCBLOnlyConflicts

        // No start or end ID:
        try {
            DocumentIterator itr;
            if (options.getKeys() != null) {
                String[] docIDs = options.getKeys().toArray(new String[options.getKeys().size()]);
                iteratorFlags |= IteratorFlags.kIncludeDeleted;
                itr = forest.iterator(docIDs, iteratorFlags);
            } else {
                String startKey;
                String endKey;
                if (options.isDescending()) {
                    startKey = (String) View.keyForPrefixMatch(
                            options.getStartKey(), options.getPrefixMatchLevel());
                    endKey = (String) options.getEndKey();
                } else {
                    startKey = (String) options.getStartKey();
                    endKey = (String) View.keyForPrefixMatch(
                            options.getEndKey(), options.getPrefixMatchLevel());
                }
                itr = forest.iterator(startKey, endKey, skip, iteratorFlags);
            }
            Document doc;
            while ((doc = itr.nextDocument()) != null) {
                try {
                    String docID = doc.getDocID();
                    if (!doc.exists()) {
                        Log.v(TAG, "AllDocs: No such row with key=\"%s\"", docID);
                        QueryRow row = new QueryRow(null, 0, docID, null, null);
                        rows.add(row);
                        continue;
                    }

                    boolean deleted = doc.deleted();
                    if (deleted &&
                            options.getAllDocsMode() != Query.AllDocsMode.INCLUDE_DELETED &&
                            options.getKeys() == null)
                        continue; // skip deleted doc
                    if (!doc.conflicted() &&
                            options.getAllDocsMode() == Query.AllDocsMode.ONLY_CONFLICTS)
                        continue; // skip non-conflicted doc
                    if (skip > 0) {
                        --skip;
                        continue;
                    }

                    String revID = doc.getSelectedRevID();
                    long sequence = doc.getSelectedSequence();

                    RevisionInternal docRevision = null;
                    if (includeDocs) {
                        // Fill in the document contents:
                        docRevision = ForestBridge.revisionObject(doc, docID, revID, true);
                        if (docRevision == null)
                            Log.w(TAG, "AllDocs: Unable to read body of doc %s", docID);
                    }

                    List<String> conflicts = new ArrayList<String>();
                    if ((options.getAllDocsMode() == Query.AllDocsMode.SHOW_CONFLICTS
                            || options.getAllDocsMode() == Query.AllDocsMode.ONLY_CONFLICTS)
                            && doc.conflicted()) {
                        conflicts = ForestBridge.getCurrentRevisionIDs(doc);
                        if (conflicts != null && conflicts.size() == 1)
                            conflicts = null;
                    }

                    Map<String, Object> value = new HashMap<String, Object>();
                    value.put("rev", revID);
                    if (deleted) // Note: In case of false, should not add for java
                        value.put("deleted", (deleted ? true : null));
                    value.put("_conflicts", conflicts);// (not found in CouchDB)

                    QueryRow row = new QueryRow(docID,
                            sequence,
                            docID,
                            value,
                            docRevision);
                    if (filter != null && !filter.apply(row)) {
                        Log.v(TAG, "   ... on 2nd thought, filter predicate skipped that row");
                        continue;
                    }
                    rows.add(row);

                    if (limit > 0 && --limit == 0)
                        break;
                } finally {
                    doc.free();
                }
            }
        } catch (ForestException e) {
            Log.e(TAG, "Error in getAllDocs()", e);
            return null;
        }

        result.put("rows", rows);
        result.put("total_rows", rows.size());
        result.put("offset", options.getSkip());
        return result;
    }

    @Override
    public RevisionList changesSince(long lastSequence,
                                     ChangesOptions options,
                                     ReplicationFilter filter,
                                     Map<String, Object> filterParams) {
        // http://wiki.apache.org/couchdb/HTTP_database_API#Changes
        if (options == null)
            options = new ChangesOptions();

        boolean withBody = (options.isIncludeDocs() || filter != null);
        int limit = options.getLimit();

        RevisionList changes = new RevisionList();
        try {
            int iteratorFlags = IteratorFlags.kDefault;
            iteratorFlags |= IteratorFlags.kIncludeDeleted;
            DocumentIterator itr = forest.iterateChanges(lastSequence, iteratorFlags);
            try {
                Document doc;
                while (limit-- > 0 && (doc = itr.nextDocument()) != null) {
                    try {
                        Log.v(TAG, "[changesSince()] docID=%s seq=%d conflicted=%s",
                                doc.getDocID(), doc.getSelectedSequence(), doc.conflicted());
                        String docID = doc.getDocID();
                        do {
                            RevisionInternal rev = ForestBridge.revisionObject(
                                    doc, docID, null, withBody);
                            if (rev == null)
                                return null;
                            if (filter == null || delegate.runFilter(filter, filterParams, rev)) {
                                if (!options.isIncludeDocs())
                                    rev.setBody(null);
                                changes.add(rev);
                            }
                        }
                        while (options.isIncludeConflicts() && doc.selectNextLeaf(true, withBody));
                    } finally {
                        doc.free();
                    }
                }
            } finally {
            }
        } catch (ForestException e) {
            Log.e(TAG, "Error in changesSince()", e);
            return null;
        }
        return changes;
    }

    ///////////////////////////////////////////////////////////////////////////
    // INSERTION / DELETION:
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public RevisionInternal add(String inDocID,
                                String inPrevRevID,
                                Map<String, Object> properties,
                                boolean deleting,
                                boolean allowConflict,
                                StorageValidation validationBlock,
                                Status outStatus)
            throws CouchbaseLiteException {
        if (outStatus != null)
            outStatus.setCode(Status.OK);

        if (readOnly)
            throw new CouchbaseLiteException(Status.FORBIDDEN);

        byte[] json;
        if (properties != null && properties.size() > 0) {
            json = RevisionUtils.asCanonicalJSON(properties);
            if (json == null)
                throw new CouchbaseLiteException(Status.BAD_JSON);
        } else {
            json = "{}".getBytes();
        }

        RevisionInternal putRev = null;
        DocumentChange change = null;

        // TODO: need to implement JNI for c4doc_put()
        // TODO: use inTransaction(Task)
        beginTransaction();
        try {
            String docID = inDocID;
            String prevRevID = inPrevRevID;

            Document doc;
            if (docID == null || docID.isEmpty())
                docID = Misc.CreateUUID();
            try {
                doc = forest.getDocument(docID, false);
            } catch (ForestException e) {
                Log.e(TAG, "ForestDB Error: getDocument(docID, false) docID=[%s]", e, docID);
                throw new CouchbaseLiteException(Status.DB_ERROR);
            }

            try {
                if (prevRevID != null) {
                    // Updating an existing revision; make sure it exists and is a leaf:
                    try {
                        if (!doc.selectRevID(prevRevID, false))
                            throw new CouchbaseLiteException(Status.NOT_FOUND);
                        if (!allowConflict && !doc.selectedRevLeaf())
                            throw new CouchbaseLiteException(Status.CONFLICT);
                    } catch (ForestException e) {
                        Log.e(TAG, "ForestDB Error: selectRevID(prevRevID, false) prevRevID=[%s]",
                                e, prevRevID);
                        throw new CouchbaseLiteException(Status.DB_ERROR);
                    }
                } else {
                    // No parent revision given:
                    if (deleting) {
                        // Didn't specify a revision to delete: NotFound or a Conflict, depending
                        throw new CouchbaseLiteException(doc.exists() ?
                                Status.CONFLICT : Status.NOT_FOUND);
                    }
                    // If doc exists, current rev must be in a deleted state or
                    // there will be a conflict:
                    if (doc.selectCurrentRev()) {
                        if (doc.selectedRevDeleted()) {
                            // New rev will be child of the tombstone:
                            // (T0D0: Write a horror novel called "Child Of The Tombstone"!)
                            prevRevID = doc.getSelectedRevID();
                        } else {
                            throw new CouchbaseLiteException(Status.CONFLICT);
                        }
                    }
                }

                // Compute the new revID.
                // (Can't be done earlier because prevRevID may have changed.)
                String newRevID = delegate.generateRevID(json, deleting, prevRevID);
                if (newRevID == null)
                    // invalid previous revID (no numeric prefix)
                    throw new CouchbaseLiteException(Status.BAD_ID);

                // Create the new CBL_Revision:
                putRev = new RevisionInternal(docID, newRevID, deleting);
                if (properties != null) {
                    properties.put("_id", docID);
                    properties.put("_rev", newRevID);
                    putRev.setProperties(properties);
                }

                // Run any validation blocks:
                if (validationBlock != null) {
                    // Fetch the previous revision and validate the new one against it:
                    RevisionInternal prevRev = null;
                    if (prevRevID != null)
                        prevRev = new RevisionInternal(docID, prevRevID, doc.selectedRevDeleted());
                    Status status = validationBlock.validate(putRev, prevRev, prevRevID);
                    if (status.isError()) {
                        outStatus.setCode(status.getCode());
                        throw new CouchbaseLiteException(status);
                    }
                }

                try {
                    if (doc.insertRevision(newRevID, json, deleting,
                            putRev.getAttachments() != null, allowConflict)) {
                        if (deleting)
                            outStatus.setCode(Status.OK); // 200
                        else
                            outStatus.setCode(Status.CREATED); // 201 (created)
                    } else
                        outStatus.setCode(Status.OK); // 200 (already exists)
                } catch (ForestException e) {
                    Log.e(TAG, "Error in insertRevision()", e);
                    throw new CouchbaseLiteException(e.code);
                }

                // Save the updated doc:
                boolean isWinner;
                try {
                    isWinner = saveForest(doc, newRevID, properties);
                } catch (ForestException e) {
                    Log.e(TAG, "Error in saveForest()", e);
                    throw new CouchbaseLiteException(Status.DB_ERROR);
                }
                putRev.setSequence(doc.getSequence());
                change = changeWithNewRevision(putRev, isWinner, doc, null);
            } finally {
                doc.free();
            }
        } finally {
            endTransaction(outStatus.isSuccessful());
        }

        if (change != null)
            delegate.databaseStorageChanged(change);
        return putRev;
    }

    /**
     * Add an existing revision of a document (probably being pulled) plus its ancestors.
     */
    @Override
    public void forceInsert(RevisionInternal inRev,
                            List<String> inHistory,
                            final StorageValidation validationBlock,
                            URL inSource)
            throws CouchbaseLiteException {
        if (readOnly)
            throw new CouchbaseLiteException(Status.FORBIDDEN);

        final byte[] json = inRev.getJson();
        if (json == null)
            throw new CouchbaseLiteException(Status.BAD_JSON);

        final RevisionInternal rev = inRev;
        final List<String> history = inHistory;
        final URL source = inSource;

        final DocumentChange[] change = new DocumentChange[1];

        // TODO: need to implement JNI for c4doc_put()
        Status status = inTransaction(new Task() {
            @Override
            public Status run() {
                // First get the CBForest doc:
                Document doc;
                try {
                    doc = forest.getDocument(rev.getDocID(), false);
                    try {
                        int common = doc.insertRevisionWithHistory(
                                json,
                                rev.isDeleted(),
                                rev.getAttachments() != null,
                                history.toArray(new String[history.size()]));
                        if (common < 0)
                            // generation numbers not in descending order
                            return new Status(Status.BAD_REQUEST);
                        else if (common == 0)
                            // No-op: No new revisions were inserted.
                            return new Status(Status.OK);
                        // Validate against the common ancestor:
                        if (validationBlock != null) {
                            RevisionInternal prev = null;
                            if (common < history.size()) {
                                String revID = history.get(common);
                                if (!doc.selectRevID(revID, false)) {
                                    Log.w(TAG, "Unable to select RevID: " + revID);
                                    return new Status(Status.BAD_REQUEST);
                                }
                                prev = new RevisionInternal(rev.getDocID(), revID, doc.deleted());
                            }
                            String parentRevID = (history.size() > 1) ? history.get(1) : null;
                            Status status = validationBlock.validate(rev, prev, parentRevID);
                            if (status.isError())
                                return status;
                        }
                        // Save updated doc back to the database:
                        boolean isWinner = saveForest(doc, history.get(0), rev.getProperties());
                        rev.setSequence(doc.getSelectedSequence());
                        change[0] = changeWithNewRevision(rev, isWinner, doc, source);
                        return new Status(Status.CREATED);
                    } finally {
                        doc.free();
                    }
                } catch (ForestException e) {
                    Log.e(TAG, "ForestDB Error: forceInsert()", e);
                    return new Status(Status.UNKNOWN);
                }
            }
        });

        if (change[0] != null)
            delegate.databaseStorageChanged(change[0]);

        if (status.isError())
            throw new CouchbaseLiteException(status.getCode());
    }

    @Override
    public Map<String, Object> purgeRevisions(Map<String, List<String>> inDocsToRevs) {
        final Map<String, Object> result = new HashMap<String, Object>();
        final Map<String, List<String>> docsToRevs = inDocsToRevs;
        Status status = inTransaction(new Task() {
            @Override
            public Status run() {
                for (String docID : docsToRevs.keySet()) {
                    List<String> revsPurged = new ArrayList<String>();
                    List<String> revIDs = docsToRevs.get(docID);
                    if (revIDs == null) {
                        return new Status(Status.BAD_PARAM);
                    } else if (revIDs.size() == 0) {
                        ; // nothing to do.
                    } else if (revIDs.contains("*")) {
                        // Delete all revisions if magic "*" revision ID is given:
                        try {
                            forest.purgeDoc(docID);
                        } catch (ForestException e) {
                            return ForestBridge.err2status(e);
                        }
                        revsPurged.add("*");
                        Log.v(TAG, "Purged doc '%s'", docID);
                    } else {
                        Document doc;
                        try {
                            doc = forest.getDocument(docID, true);
                        } catch (ForestException e) {
                            return ForestBridge.err2status(e);
                        }
                        try {
                            List<String> purged = new ArrayList<String>();
                            for (String revID : revIDs) {
                                try {
                                    if (doc.purgeRevision(revID) > 0)
                                        purged.add(revID);
                                } catch (ForestException e) {
                                    Log.e(TAG, "error in purgeRevision()", e);
                                }
                            }
                            if (purged.size() > 0) {
                                try {
                                    doc.save(maxRevTreeDepth);
                                } catch (ForestException e) {
                                    return ForestBridge.err2status(e);
                                }
                                Log.v(TAG, "Purged doc '%s' revs '%s'", docID, revIDs);
                            }
                            revsPurged = purged;
                        } finally {
                            doc.free();
                        }
                    }
                    result.put(docID, revsPurged);
                }
                return new Status(Status.OK);
            }
        });
        return result;
    }

    @Override
    public ViewStore getViewStorage(String name, boolean create) throws CouchbaseLiteException {
        return new ForestDBViewStore(this, name, create);
    }

    @Override
    public List<String> getAllViewNames() {
        List<String> result = new ArrayList<String>();
        String[] fileNames = new File(directory).list();
        for (String filename : fileNames) {
            try {
                result.add(ForestDBViewStore.fileNameToViewName(filename));
            } catch (CouchbaseLiteException e) {
                Log.i(TAG, "Invalid filename as a view store: filename=" + filename);
            }
        }
        return result;
    }

    @Override
    public RevisionInternal getLocalDocument(String docID, String revID) {
        if (docID == null || !docID.startsWith("_local/"))
            return null;
        byte[][] metaNbody;
        try {
            metaNbody = forest.rawGet("_local", docID);
        } catch (ForestException e) {
            return null;
        }

        // meta -> revID
        String gotRevID = new String(metaNbody[0]);
        if (gotRevID == null || (revID != null && !revID.equals(gotRevID)))
            return null;

        // body -> properties
        Map<String, Object> properties = getDocProperties(metaNbody[1]);
        if (properties == null)
            return null;

        properties.put("_id", docID);
        properties.put("_rev", gotRevID);
        RevisionInternal result = new RevisionInternal(docID, gotRevID, false);
        result.setProperties(properties);
        return result;
    }

    @Override
    public RevisionInternal putLocalRevision(final RevisionInternal revision,
                                             final String prevRevID,
                                             final boolean obeyMVCC)
            throws CouchbaseLiteException {
        final String docID = revision.getDocID();
        if (!docID.startsWith("_local/"))
            throw new CouchbaseLiteException(Status.BAD_ID);

        if (revision.isDeleted()) {
            // DELETE:
            Status status = deleteLocalDocument(docID, prevRevID, obeyMVCC);
            if (status.isSuccessful())
                return revision;
            else
                throw new CouchbaseLiteException(status.getCode());
        } else {
            // PUT:
            final RevisionInternal[] result = new RevisionInternal[1];
            Status status = inTransaction(new Task() {
                @Override
                public Status run() {
                    byte[] json = revision.getJson();
                    if (json == null)
                        return new Status(Status.BAD_JSON);

                    byte[][] metaNbody = null;
                    try {
                        metaNbody = forest.rawGet("_local", docID);
                    } catch (ForestException e) {
                    }

                    int generation = RevisionInternal.generationFromRevID(prevRevID);
                    if (obeyMVCC) {
                        if (prevRevID != null) {
                            if (metaNbody != null && !prevRevID.equals(new String(metaNbody[0])))
                                return new Status(Status.CONFLICT);
                            if (generation == 0)
                                return new Status(Status.BAD_ID);
                        } else {
                            if (metaNbody != null)
                                return new Status(Status.CONFLICT);
                        }
                    }
                    String newRevID = String.format("%d-local", generation + 1);
                    try {
                        forest.rawPut("_local", docID, newRevID.getBytes(), json);
                    } catch (ForestException e) {
                        return ForestBridge.err2status(e);
                    }
                    result[0] = revision.copyWithDocID(docID, newRevID);
                    return new Status(Status.CREATED);
                }
            });

            if (status.isSuccessful())
                return result[0];
            else
                throw new CouchbaseLiteException(status.getCode());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal (PROTECTED & PRIVATE) METHODS
    ///////////////////////////////////////////////////////////////////////////

    private boolean saveForest(Document doc,
                               String revID,
                               Map<String, Object> properties)
            throws ForestException {
        // after insertRevision, the selected revision is inserted revision.
        // need to select current revision.
        doc.selectCurrentRev();
        // Is the new revision the winner?
        boolean isWinner = doc.getSelectedRevID().equalsIgnoreCase(revID);
        // Update the documentType:
        if (!isWinner)
            properties = ForestBridge.bodyOfSelectedRevision(doc);
        if (properties != null && properties.containsKey("type") &&
                properties.get("type") instanceof String)
            doc.setType((String) properties.get("type"));
        // save
        doc.save(maxRevTreeDepth);
        return isWinner;
    }

    private DocumentChange changeWithNewRevision(RevisionInternal inRev,
                                                 boolean isWinningRev,
                                                 Document doc,
                                                 URL source) {
        String winningRevID = isWinningRev ? inRev.getRevID() : doc.getSelectedRevID();
        return new DocumentChange(inRev, winningRevID, doc.conflicted(), source);
    }

    private boolean beginTransaction() {
        try {
            forest.beginTransaction();
            transactionLevel4Thread.set(transactionLevel4Thread.get() + 1);
        } catch (ForestException e) {
            Log.e(TAG, "Failed to begin transaction", e);
            return false;
        }
        return true;
    }

    private boolean endTransaction(boolean commit) {
        try {
            transactionLevel4Thread.set(transactionLevel4Thread.get() - 1);
            forest.endTransaction(commit);
        } catch (ForestException e) {
            Log.e(TAG, "Failed to end transaction", e);
            return false;
        }
        delegate.storageExitedTransaction(commit);
        return true;
    }

    private static Map<String, Object> getDocProperties(byte[] body) {
        try {
            return Manager.getObjectMapper().readValue(body, Map.class);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * CBLDatabase+LocalDocs.m
     * - (CBLStatus) deleteLocalDocumentWithID: (NSString*)docID
     * revisionID: (NSString*)revID
     * obeyMVCC: (BOOL)obeyMVCC;
     */
    private Status deleteLocalDocument(
            final String inDocID, String inRevID, final boolean obeyMVCC) {
        final String docID = inDocID;
        final String revID = inRevID;

        if (docID == null || !docID.startsWith("_local/"))
            return new Status(Status.BAD_ID);
        if (obeyMVCC && revID == null)
            // Didn't specify a revision to delete: 404 or a 409, depending
            return new Status(getLocalDocument(docID, null) != null ?
                    Status.CONFLICT : Status.NOT_FOUND);

        return inTransaction(new Task() {
            @Override
            public Status run() {
                try {
                    byte[][] metaNbody = forest.rawGet("_local", docID);
                    if (metaNbody == null) {
                        return new Status(Status.NOT_FOUND);
                    } else if (obeyMVCC && revID != null &&
                            !revID.equals(new String(metaNbody[0]))) {
                        return new Status(Status.CONFLICT);
                    } else {
                        forest.rawPut("_local", docID, null, null);
                        return new Status(Status.OK);
                    }
                } catch (ForestException e) {
                    return ForestBridge.err2status(e);
                }
            }
        });
    }

    @Override
    public void setEncryptionKey(SymmetricKey key) {
        encryptionKey = key;
    }

    @Override
    public SymmetricKey getEncryptionKey() {
        return encryptionKey;
    }

    @Override
    public Action actionToChangeEncryptionKey(final SymmetricKey newKey) {
        Action action = new Action();

        // Re-key the views!
        List<String> viewNames = getAllViewNames();
        for (String viewName : viewNames) {
            try {
                ForestDBViewStore viewStorage = (ForestDBViewStore) getViewStorage(viewName, true);
                action.add(viewStorage.getActionToChangeEncryptionKey());
            } catch (CouchbaseLiteException ex) {
                Log.w(TAG, "Error in getViewStorage() viewName=" + viewName, ex);
            }
        }

        // Re-key the database:
        final SymmetricKey oldKey = encryptionKey;
        action.add(
                new ActionBlock() {
                    @Override
                    public void execute() throws ActionException {
                        int algorithm = Database.NoEncryption;
                        byte[] key = null;
                        if (newKey != null) {
                            algorithm = Database.AES256Encryption;
                            key = newKey.getKey();
                        }
                        try {
                            forest.rekey(algorithm, key);
                            setEncryptionKey(newKey);
                        } catch (ForestException e) {
                            throw new ActionException("Cannot rekey to the new key", e);
                        }
                    }
                },
                new ActionBlock() {
                    @Override
                    public void execute() throws ActionException {
                        int algorithm = Database.NoEncryption;
                        byte[] key = null;
                        if (oldKey != null) {
                            algorithm = Database.AES256Encryption;
                            key = newKey.getKey();
                        }
                        try {
                            // FIX: This can potentially fail. If it did, the database would be lost
                            // It would be safer to save & restore the old db file, the one that
                            // got replaced
                            // during rekeying, but the ForestDB API doesn't allow preserving it...
                            forest.rekey(algorithm, key);
                            setEncryptionKey(oldKey);
                        } catch (ForestException e) {
                            throw new ActionException("Cannot rekey to the old key", e);
                        }
                    }
                }, null
        );

        return action;
    }

    @Override
    public byte[] derivePBKDF2SHA256Key(String password, byte[] salt, int rounds)
            throws CouchbaseLiteException {
        byte[] key = nativeDerivePBKDF2SHA256Key(password, salt, rounds);
        if (key == null)
            throw new CouchbaseLiteException("Cannot derive key for the password",
                    Status.BAD_REQUEST);
        return key;
    }

    private interface Task {
        Status run();
    }

    private Status inTransaction(Task task) {
        if (inTransaction())
            return task.run();
        else {
            Status status = new Status(Status.OK);
            boolean commit = false;
            beginTransaction();
            try {
                status = task.run();
                commit = !status.isError();
            } finally {
                endTransaction(commit);
            }
            return status;
        }
    }
}
