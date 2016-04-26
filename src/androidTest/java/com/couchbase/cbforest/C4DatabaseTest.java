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
package com.couchbase.cbforest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Ported from c4DatabaseTest.cc
 */
public class C4DatabaseTest extends C4TestCase {

    public void testForestException() {
        try {
            new Database("", 0, 0, null);
            fail();
        } catch (ForestException e) {
            assertEquals(Constants.C4ErrorDomain.ForestDBDomain, e.domain);
            assertEquals(Constants.FDBErrors.FDB_RESULT_NO_SUCH_FILE, e.code);
            assertEquals("no such file", e.getMessage());
        }

        try {
            db.getDocument("a", true);
            fail();
        } catch (ForestException e) {
            assertEquals(Constants.C4ErrorDomain.ForestDBDomain, e.domain);
            assertEquals(Constants.FDBErrors.FDB_RESULT_KEY_NOT_FOUND, e.code);
            assertEquals("key not found", e.getMessage());
        }

        try {
            db.getDocument(null, true);
            fail();
        } catch (ForestException e) {
            assertEquals(Constants.C4ErrorDomain.ForestDBDomain, e.domain);
            assertEquals(Constants.FDBErrors.FDB_RESULT_INVALID_ARGS, e.code);
            assertEquals("invalid arguments", e.getMessage());
        }
    }

    public void testTransaction() throws ForestException {
        assertEquals(0, db.getDocumentCount());
        assertFalse(db.isInTransaction());
        db.beginTransaction();
        assertTrue(db.isInTransaction());
        db.beginTransaction();
        assertTrue(db.isInTransaction());
        db.endTransaction(true);
        assertTrue(db.isInTransaction());
        db.endTransaction(true);
        assertFalse(db.isInTransaction());
    }

    public void testCreateRawDoc() throws ForestException {
        // 1. normal case
        final String store = "test";
        final String key = "key";
        final String meta = "meta";
        boolean commit = false;
        db.beginTransaction();
        try {
            db.rawPut(store, key, meta.getBytes(), kBody.getBytes());
            commit = true;
        } finally {
            db.endTransaction(commit);
        }
        byte[][] metaNbody = db.rawGet(store, key);
        assertNotNull(metaNbody);
        assertEquals(2, metaNbody.length);
        assertTrue(Arrays.equals(meta.getBytes(), metaNbody[0]));
        assertTrue(Arrays.equals(kBody.getBytes(), metaNbody[1]));
    }

    public void testCreateRawDocWithNullValue() throws ForestException {
        final String store = "test";
        final String key = "key";
        final String meta = "meta";

        // 2. null meta
        boolean commit = false;
        db.beginTransaction();
        try {
            db.rawPut(store, key, null, kBody.getBytes());
            commit = true;
        } finally {
            db.endTransaction(commit);
        }
        byte[][] metaNbody = db.rawGet(store, key);
        assertNotNull(metaNbody);
        assertEquals(2, metaNbody.length);
        assertTrue(Arrays.equals(null, metaNbody[0]));
        assertTrue(Arrays.equals(kBody.getBytes(), metaNbody[1]));

        // 3. null body
        commit = false;
        db.beginTransaction();
        try {
            db.rawPut(store, key, meta.getBytes(), null);
            commit = true;
        } finally {
            db.endTransaction(commit);
        }
        metaNbody = db.rawGet(store, key);
        assertNotNull(metaNbody);
        assertEquals(2, metaNbody.length);
        assertTrue(Arrays.equals(meta.getBytes(), metaNbody[0]));
        assertTrue(Arrays.equals(null, metaNbody[1]));

        // null meta and null body -> it delete
        commit = false;
        db.beginTransaction();
        try {
            db.rawPut(store, key, null, null);
            commit = true;
        } finally {
            db.endTransaction(commit);
        }

        try {
            db.rawGet(store, key);
            fail("ForestException should be thrown");
        } catch (ForestException e) {
            assertEquals(Constants.C4ErrorDomain.ForestDBDomain, e.domain);
            assertEquals(FDBErrors.FDB_RESULT_KEY_NOT_FOUND, e.code);
        }
    }

    public void testCreateVersionedDoc() throws ForestException {
        // Try reading doc with mustExist=true, which should fail:
        Document doc = null;
        try {
            doc = db.getDocument(kDocID, true);
            fail("Should be thrown ForestException");
        } catch (ForestException e) {
            assertEquals(FDBErrors.FDB_RESULT_KEY_NOT_FOUND, e.code);
            assertEquals(C4ErrorDomain.ForestDBDomain, e.domain);
        }
        assertNull(doc);

        // Now get the doc with mustExist=false, which returns an empty doc:
        doc = db.getDocument(kDocID, false);
        assertNotNull(doc);
        assertEquals(0, doc.getFlags());
        assertEquals(kDocID, doc.getDocID());
        assertNull(doc.getRevID());
        {
            boolean commit = false;
            db.beginTransaction();
            try {
                doc.insertRevision(kRevID, kBody.getBytes(), false, false, false);
                assertEquals(kRevID, doc.getRevID());
                assertEquals(kRevID, doc.getSelectedRevID());
                assertEquals(C4RevisionFlags.kRevNew | C4RevisionFlags.kRevLeaf, doc.getSelectedRevFlags());
                assertTrue(Arrays.equals(kBody.getBytes(), doc.getSelectedBody()));
                doc.save(20);   // 20 is default value of maxRevTreeDepth which is defined in Database.java and ForestDBStore.java
                commit = true;
            } finally {
                db.endTransaction(commit);
            }
        }
        doc.free();

        // Reload the doc:
        doc = db.getDocument(kDocID, true);
        assertNotNull(doc);
        assertEquals(C4DocumentFlags.kExists, doc.getFlags());
        assertEquals(kDocID, doc.getDocID());
        assertEquals(kRevID, doc.getRevID());
        assertEquals(kRevID, doc.getSelectedRevID());
        assertEquals(1, doc.getSelectedSequence());
        assertTrue(Arrays.equals(kBody.getBytes(), doc.getSelectedBody()));
        doc.free();

        // Get the doc by its sequence
        doc = db.getDocumentBySequence(1);
        assertNotNull(doc);
        assertEquals(C4DocumentFlags.kExists, doc.getFlags());
        assertEquals(kDocID, doc.getDocID());
        assertEquals(kRevID, doc.getRevID());
        assertEquals(kRevID, doc.getSelectedRevID());
        assertEquals(1, doc.getSelectedSequence());
        assertTrue(Arrays.equals(kBody.getBytes(), doc.getSelectedBody()));
    }

    public void testCreateMultipleRevisions() throws ForestException {
        final String kBody2 = "{\"ok\":\"go\"}";
        createRev(kDocID, kRevID, kBody.getBytes());
        createRev(kDocID, kRev2ID, kBody2.getBytes());
        createRev(kDocID, kRev2ID, kBody2.getBytes(), false);// test redundant insert

        // Reload the doc:
        Document doc = db.getDocument(kDocID, true);
        assertNotNull(doc);
        assertEquals(C4DocumentFlags.kExists, doc.getFlags());
        assertEquals(kDocID, doc.getDocID());
        assertEquals(kRev2ID, doc.getRevID());
        assertEquals(kRev2ID, doc.getSelectedRevID());
        assertEquals(2, doc.getSelectedSequence());
        assertTrue(Arrays.equals(kBody2.getBytes(), doc.getSelectedBody()));

        // Select 1st revision:
        assertTrue(doc.selectParentRev());
        assertEquals(kRevID, doc.getSelectedRevID());
        assertEquals(1, doc.getSelectedSequence());
        assertNull(doc.getSelectedBodyTest());
        assertTrue(Arrays.equals(kBody.getBytes(), doc.getSelectedBody()));
        assertFalse(doc.selectParentRev());

        // Compact database:
        db.compact();

        // Reload the doc:
        doc = db.getDocument(kDocID, true);
        assertNotNull(doc);
        assertTrue(doc.selectParentRev());
        assertEquals(kRevID, doc.getSelectedRevID());
        assertEquals(1, doc.getSelectedSequence());
        assertNull(doc.getSelectedBodyTest());
        assertFalse(doc.hasRevisionBody());
        try {
            doc.getSelectedBody();
            fail("should be thrown exception");
        } catch (ForestException e) {
            assertEquals(Constants.C4ErrorDomain.HTTPDomain, e.domain);
            assertEquals(410, e.code);
        }

        // Purge doc
        {
            boolean commit = false;
            db.beginTransaction();
            try {
                int nPurged = doc.purgeRevision(kRev2ID);
                assertEquals(2, nPurged);
                doc.save(20);
                commit = true;
            } finally {
                db.endTransaction(commit);
            }
        }
    }

    // JNI does not wrap c4doc_getForPut
    //public void testGetForPut() throws ForestException {
    //}

    public void testInsertRevisionWithHistory() throws ForestException {
        _testInsertRevisionWithHistory(20);
    }

    public void testInsertRevisionWith512History() throws ForestException {
        _testInsertRevisionWithHistory(512);
    }

    public void testInsertRevisionWith1024History() throws ForestException {
        _testInsertRevisionWithHistory(1024);
    }

    public void _testInsertRevisionWithHistory(int kHistoryCount) throws ForestException {
        String kBody2 = "{\"ok\":\"go\"}";
        createRev(kDocID, kRevID, kBody.getBytes());
        createRev(kDocID, kRev2ID, kBody2.getBytes());

        // Reload the doc:
        Document doc = db.getDocument(kDocID, true);

        // Add 18 revisions; the last two entries in the history repeat the two existing revs:
        Random r = new Random();
        List<String> revIDs = new ArrayList<String>();
        for (int i = kHistoryCount - 1; i >= 2; i--) {
            String revID = String.format("%d-%08x", i + 1, r.nextInt());
            revIDs.add(revID);
        }
        revIDs.add(kRev2ID);
        revIDs.add(kRevID);

        int n;
        {
            boolean commit = false;
            db.beginTransaction();
            try {
                n = doc.insertRevisionWithHistory("{\"foo\":true}".getBytes(),
                        false, false,
                        revIDs.toArray(new String[revIDs.size()]));
                commit = true;
            } finally {
                db.endTransaction(commit);
            }
        }
        assertEquals(kHistoryCount - 2, n);
    }

    // JNI does not wrap c4doc_put
    //public void testPut() throws ForestException {
    //}

    private void setupAllDocs() throws ForestException {
        for (int i = 1; i < 100; i++) {
            String docID = String.format("doc-%03d", i);
            createRev(docID, kRevID, kBody.getBytes());
        }

        // Add a deleted doc to make sure it's skipped by default:
        createRev("doc-005DEL", kRevID, null);
    }

    public void testAllDocs() throws ForestException {
        setupAllDocs();

        // No start or end ID:
        int iteratorFlags = IteratorFlags.kDefault;
        iteratorFlags &= ~IteratorFlags.kIncludeBodies;
        DocumentIterator itr = db.iterator(null, null, 0, iteratorFlags);
        assertNotNull(itr);
        Document doc;
        int i = 1;
        while ((doc = itr.nextDocument()) != null) {
            try {
                String docID = String.format("doc-%03d", i);
                assertEquals(docID, doc.getDocID());
                assertEquals(kRevID, doc.getRevID());
                assertEquals(kRevID, doc.getSelectedRevID());
                assertEquals(i, doc.getSelectedSequence());
                assertNull(doc.getSelectedBodyTest());
                // Doc was loaded without its body, but it should load on demand:
                assertTrue(Arrays.equals(kBody.getBytes(), doc.getSelectedBody()));
                i++;
            } finally {
                doc.free();
            }
        }
        assertEquals(100, i);

        // Start and end ID:
        itr = db.iterator("doc-007", "doc-090", 0, IteratorFlags.kDefault);
        assertNotNull(itr);
        i = 7;
        while ((doc = itr.nextDocument()) != null) {
            try {
                String docID = String.format("doc-%03d", i);
                assertEquals(docID, doc.getDocID());
                i++;
            } finally {
                doc.free();
            }
        }
        assertEquals(91, i);

        // Some docs, by ID:
        String[] docIDs = {"doc-042", "doc-007", "bogus", "doc-001"};
        iteratorFlags = IteratorFlags.kDefault;
        iteratorFlags |= IteratorFlags.kIncludeDeleted;
        itr = db.iterator(docIDs, iteratorFlags);
        assertNotNull(itr);
        i = 0;
        while ((doc = itr.nextDocument()) != null) {
            try {
                assertEquals(docIDs[i], doc.getDocID());
                assertEquals(i != 2, doc.getSelectedSequence() != 0);
                i++;
            } finally {
                doc.free();
            }
        }
        assertEquals(4, i);
    }

    public void testAllDocsIncludeDeleted() throws ForestException {
        setupAllDocs();
        int iteratorFlags = IteratorFlags.kDefault;
        iteratorFlags |= IteratorFlags.kIncludeDeleted;
        DocumentIterator itr = db.iterator("doc-004", "doc-007", 0, iteratorFlags);
        assertNotNull(itr);
        Document doc;
        int i = 4;
        while ((doc = itr.nextDocument()) != null) {
            try {
                String docID;
                if (i == 6)
                    docID = "doc-005DEL";
                else
                    docID = String.format("doc-%03d", i >= 6 ? i - 1 : i);
                assertEquals(docID, doc.getDocID());
                i++;
            } finally {
                doc.free();
            }
        }
        assertEquals(9, i);
    }

    public void testAllDocsInfo() throws ForestException {
        setupAllDocs();

        // No start or end ID:
        int iteratorFlags = IteratorFlags.kDefault;
        DocumentIterator itr = db.iterator(null, null, 0, iteratorFlags);
        assertNotNull(itr);
        Document doc;
        int i = 1;
        while ((doc = itr.nextDocument()) != null) {
            try {
                String docID = String.format("doc-%03d", i);
                assertEquals(docID, doc.getDocID());
                assertEquals(kRevID, doc.getRevID());
                assertEquals(kRevID, doc.getSelectedRevID());
                assertEquals(i, doc.getSequence());
                assertEquals(i, doc.getSelectedSequence());
                assertEquals(C4DocumentFlags.kExists, doc.getFlags());
                i++;
            } finally {
                doc.free();
            }
        }
        assertEquals(100, i);
    }

    public void testChanges() throws ForestException {
        for (int i = 1; i < 100; i++) {
            String docID = String.format("doc-%03d", i);
            createRev(docID, kRevID, kBody.getBytes());
        }

        // Since start:
        int iteratorFlags = IteratorFlags.kDefault;
        iteratorFlags &= ~IteratorFlags.kIncludeBodies;
        DocumentIterator itr = new DocumentIterator(db._handle, 0, iteratorFlags);
        assertNotNull(itr);
        Document doc;
        long seq = 1;
        while ((doc = itr.nextDocument()) != null) {
            try {
                String docID = String.format("doc-%03d", seq);
                assertEquals(docID, doc.getDocID());
                assertEquals(seq, doc.getSelectedSequence());
                seq++;
            } finally {
                doc.free();
            }
        }
        assertEquals(100L, seq);

        // Since 6:
        itr = new DocumentIterator(db._handle, 6, iteratorFlags);
        assertNotNull(itr);
        seq = 7;
        while ((doc = itr.nextDocument()) != null) {
            try {
                String docID = String.format("doc-%03d", seq);
                assertEquals(docID, doc.getDocID());
                assertEquals(seq, doc.getSelectedSequence());
                seq++;
            } finally {
                doc.free();
            }
        }
        assertEquals(100L, seq);
    }
}
