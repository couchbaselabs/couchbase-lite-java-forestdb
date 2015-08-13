package com.couchbase.lite.cbforest;

import android.util.Log;

import java.math.BigInteger;

/**
 * Created by hideki on 8/13/15.
 */
public class VersionedDocumentTest extends BaseCBForestTestCase {
    public static final String TAG = VersionedDocumentTest.class.getSimpleName();

    public void test01_Empty() throws Exception {
        VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));
        assertEquals("foo", new String(v.getDocID().getBuf()));
        Log.i(TAG, "v.getRevID()=" + v.getRevID());
        Log.i(TAG, "v.getRevID()=" + v.getRevID().getBuf());
        assertNull(v.getRevID().getBuf());
        assertEquals(0, v.getFlags());
        Revision rev = v.get(new RevIDBuffer(new Slice("1-aaaa".getBytes())));
        assertNull(rev);
        v.delete();
    }

    // CouchBase Lite does not call RevTree directly. Only through VersionedDocument
    public void test02_RevTreeInsert() throws Exception {

        RevTree tree = new RevTree();
        RevIDBuffer rev1ID = new RevIDBuffer(new Slice("1-aaaa".getBytes()));
        Slice rev1Data = new Slice("body of revision".getBytes());
        Revision rev = tree.insert(rev1ID, rev1Data, false, false, new RevID(), false);
        assertNotNull(rev);
        assertEquals(201, tree.getLatestHttpStatus());
        assertTrue(rev.getRevID().compare(rev1ID) == 0);
        assertNull(rev.getParent());
        assertFalse(rev.isDeleted());
        Log.i(TAG, "rev.getRevID() => " + rev.getRevID());

        RevIDBuffer rev2ID = new RevIDBuffer(new Slice("2-bbbb".getBytes()));
        Slice rev2Data = new Slice("second revision".getBytes());
        Revision rev2 = tree.insert(rev2ID, rev2Data, false, false, rev1ID, false);
        assertNotNull(rev2);
        assertEquals(201, tree.getLatestHttpStatus());
        assertTrue(rev2.getRevID().compare(rev2ID) == 0);
        assertFalse(rev2.isDeleted());
        Log.i(TAG, "rev2.getRevID() => " + rev2.getRevID());

        tree.sort();

        rev = tree.get(rev1ID);
        rev2 = tree.get(rev2ID);

        assertNotNull(rev);
        assertNotNull(rev2);
        assertNotNull(rev2.getParent());
        assertNull(rev.getParent());
        assertTrue(rev2.getParent().isSameAddress(rev));
        assertTrue(rev2.getParent().getRevID().compare(rev.getRevID()) == 0);
        assertTrue(tree.currentRevision().isSameAddress(rev2));
        assertFalse(tree.hasConflict());

        tree.sort();

        assertTrue(tree.get(0).isSameAddress(rev2));
        assertTrue(tree.get(1).isSameAddress(rev));
        assertEquals(1, rev.index());
        assertEquals(0, rev2.index());

        Slice ext = tree.encode();
        RevTree tree2 = new RevTree(ext, BigInteger.valueOf(12), BigInteger.valueOf(1234));
    }

    public void test03_AddRevision() throws Exception {

        String revID = "1-fadebead";
        String body = "{\"hello\":true}";
        RevIDBuffer revIDBuf = new RevIDBuffer(new Slice(revID.getBytes()));
        VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));
        v.insert(revIDBuf, new Slice(body.getBytes()), false, false, (Revision)null, false);
        assertEquals(201, v.getLatestHttpStatus());
        Revision node = v.get(new RevIDBuffer(new Slice(revID.getBytes())));
        assertNotNull(node);
        assertFalse(node.isDeleted());
        assertTrue(node.isLeaf());
        assertTrue(node.isActive());
        assertEquals(1, v.size());
        VectorRevision revs = v.currentRevisions();
        assertEquals(1, revs.size());
        Revision rev1 = revs.get(0);
        Revision rev2 = v.currentRevision();
        assertTrue(rev1.isSameAddress(rev2));

    }

    public void test04_DocType() throws Exception {

        RevIDBuffer rev1ID = new RevIDBuffer(new Slice("1-aaaa".getBytes()));

        {
            VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));

            Slice rev1Data = new Slice("body of revision".getBytes());

            v.insert(rev1ID, rev1Data, true/*deleted*/, false, new RevID(), false);
            //int httpStatus = v.getLatestHttpStatus();
            v.setDocType(new Slice("moose".getBytes()));
            assertEquals("moose", new String(v.getDocType().getBuf()));
            Transaction t =  new Transaction(db);
            v.save(t);
            t.delete();
        }

        {
            VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));
            assertEquals(VersionedDocument.kDeleted, v.getFlags());
            assertTrue(v.getRevID().compare(rev1ID) == 0);
            assertEquals("moose", new String(v.getDocType().getBuf()));
        }
    }
}
