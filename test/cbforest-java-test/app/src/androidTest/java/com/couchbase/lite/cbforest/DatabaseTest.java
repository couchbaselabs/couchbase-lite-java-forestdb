package com.couchbase.lite.cbforest;

import java.io.File;
import java.math.BigInteger;

/**
 * Created by hideki on 8/12/15.
 */
public class DatabaseTest extends BaseCBForestTestCase {
    public static final String TAG = DatabaseTest.class.getSimpleName();

    public void test01_DbInfo() throws Exception {
        FileInfo info = db.getFileInfo();
        assertEquals(0, info.getDocCount().intValue());
        assertEquals(0, info.getSpaceUsed().intValue());
        assertTrue(info.getFileSize().intValue() > 0);
        assertEquals(0, db.getLastSequence().intValue());
    }

    public void test02_CreateDoc() throws Exception {
        Slice key = new Slice("key".getBytes());
        {
            Transaction t = new Transaction(db);
            t.set(key, new Slice("value".getBytes()));
            t.delete();
        }
        assertEquals(1, db.getLastSequence().intValue());
        Document doc = db.get(key);
        assertTrue(doc.getKey().compare(key) == 0);
        assertTrue(doc.getBody().compare(new Slice("value".getBytes())) == 0);
        doc.delete();
        key.delete();
    }

    public void test03_SaveDocs() throws Exception {
        Document tmp_doc;

        //WORKAROUND: Add a doc before the main transaction so it doesn't start at sequence 0
        Transaction tmp_t = new Transaction(db);
        tmp_t.set(new Slice("a".getBytes()), new Slice("A".getBytes()));
        tmp_t.delete();

        // test with another database instance which points to same database file
        File dbFile = new File(mContext.getFilesDir(), DB_FILENAME);
        Database aliased_db = new Database(dbFile.getPath(), Database.defaultConfig());

        tmp_doc = aliased_db.get(new Slice("a".getBytes()));
        assertNotNull(tmp_doc);
        assertTrue(tmp_doc.getKey().compare(new Slice("a".getBytes())) == 0);
        assertTrue(tmp_doc.getBody().compare(new Slice("A".getBytes())) == 0);
        tmp_doc.delete();

        {
            Transaction t = new Transaction(db);
            Document doc = new Document(new Slice("doc".getBytes()));
            doc.setMeta(new Slice("m-e-t-a".getBytes()));
            doc.setBody(new Slice("THIS IS THE BODY".getBytes()));
            t.write(doc);

            assertEquals(BigInteger.valueOf(2), doc.getSequence());
            assertEquals(2, db.getLastSequence().intValue());

            Document doc_alias = t.get(doc.getSequence());
            assertNotNull(doc_alias);
            assertEquals(new String(doc_alias.getKey().getBuf()), new String(doc.getKey().getBuf()));
            assertEquals(new String(doc_alias.getMeta().getBuf()), new String(doc.getMeta().getBuf()));
            assertEquals(new String(doc_alias.getBody().getBuf()), new String(doc.getBody().getBuf()));

            doc_alias.setBody(new Slice("NU BODY".getBytes()));
            t.write(doc_alias);

            assertTrue(t.read(doc));
            assertEquals(BigInteger.valueOf(3), doc.getSequence());
            assertEquals(new String(doc_alias.getMeta().getBuf()), new String(doc.getMeta().getBuf()));
            assertEquals(new String(doc_alias.getBody().getBuf()), new String(doc.getBody().getBuf()));
            doc_alias.delete();

            // Doc shouldn't exist outside transaction yet:
            tmp_doc = aliased_db.get(new Slice("doc".getBytes()));
            assertEquals(0, tmp_doc.getSequence().intValue());
            tmp_doc.delete();

            // Release Transaction 2
            t.delete();
        }

        tmp_doc = db.get(new Slice("doc".getBytes()));
        assertEquals(3, tmp_doc.getSequence().intValue());
        tmp_doc.delete();

        tmp_doc = aliased_db.get(new Slice("doc".getBytes()));
        assertEquals(3, tmp_doc.getSequence().intValue());
        tmp_doc.delete();

        aliased_db.delete();
    }
}
