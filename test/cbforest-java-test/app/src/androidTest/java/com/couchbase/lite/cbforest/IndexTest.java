package com.couchbase.lite.cbforest;

import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hideki on 8/13/15.
 */
public class IndexTest extends BaseCBForestTestCase {
    public static final String TAG = IndexTest.class.getSimpleName();
    Index index = null;
    BigInteger _rowCount = null;

    protected void setUp() throws Exception {
        super.setUp();
        index = new Index(db, "index");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if(index != null){
            index.delete();
            index = null;
        }
    }
    void updateDoc(String docID, List<String> body, IndexWriter writer) throws Exception {
        Log.i(TAG, "updateDoc " + docID + " body=" + Arrays.toString(body.toArray()));

        VectorCollatable keys = new VectorCollatable();
        VectorCollatable values = new VectorCollatable();
        for(int i = 1; i < body.size(); i++){
            Collatable key = new Collatable();
            key.add(body.get(i));
            keys.add(key);

            Log.i(TAG, "updateDoc() body.get(i) = " + body.get(i));
            // key is binary, should not print!
            CollatableReader r = new CollatableReader(key.toSlice());
            Log.i(TAG, "updateDoc() key = " + new String(r.readString().getBuf()));

            Collatable value = new Collatable();
            value.add(body.get(0));
            values.add(value);
        }
        assertTrue(writer.update(new Slice(docID.getBytes()), BigInteger.valueOf(1), keys, values));
    }

    int doQuery() throws Exception {
        int nRows = 0;
        IndexEnumerator e = new IndexEnumerator(index,
                new Collatable(), new Slice(),
                new Collatable(), new Slice(),
                new DocEnumerator.Options());
        for ( ; e.next(); nRows++) {
            Log.i(TAG, String.format("key = %s, value = %s, docID = %s",
                    new String(e.key().readString().getBuf()),
                    new String(e.value().readString().getBuf()),
                    new String(e.docID().getBuf())));
        }
        assertEquals(_rowCount.intValue(), nRows);
        return nRows;
    }

    public void testBasics() throws Exception {
        Map<String, List<String>> docs = new HashMap<String, List<String>>();
        docs.put("CA", Arrays.asList("California", "San Jose", "San Francisco", "Cambria"));
        docs.put("WA", Arrays.asList("Washington", "Seattle", "Port Townsend", "Skookumchuk"));
        docs.put("OR", Arrays.asList("Oregon", "Portland", "Eugene"));

        {
            Log.i(TAG, "--- Populate index");
            Transaction trans = new Transaction(db);
            IndexWriter writer = new IndexWriter(index, trans);
            for (String docID : docs.keySet())
                updateDoc(docID, docs.get(docID), writer);
            _rowCount = writer.getRowCount();
            writer.delete();
            trans.delete();
        }

        Log.i(TAG, "--- First query");
        assertEquals(8, doQuery());
        {
            Log.i(TAG, "--- Updating OR");
            Transaction trans = new Transaction(db);
            IndexWriter writer = new IndexWriter(index, trans);
            writer.setRowCount(_rowCount);
            updateDoc("OR", Arrays.asList("Oregon", "Portland", "Walla Walla", "Salem"), writer);
            _rowCount = writer.getRowCount();
            writer.delete();
            trans.delete();
        }
        assertEquals(9, doQuery());

        {
            Log.i(TAG, "--- Removing CA");
            Transaction trans = new Transaction(db);
            IndexWriter writer = new IndexWriter(index, trans);
            writer.setRowCount(_rowCount);
            updateDoc("CA", new ArrayList<String>(), writer);
            _rowCount = writer.getRowCount();
            writer.delete();
            trans.delete();
        }
        assertEquals(6, doQuery());

        Log.i(TAG, "--- Reverse enumeration");
        int nRows = 0;
        DocEnumerator.Options options = new DocEnumerator.Options();
        options.setDescending(true);
        IndexEnumerator e = new IndexEnumerator(index,
                new Collatable(), new Slice(),
                new Collatable(), new Slice(),
                options);
        for ( ; e.next(); nRows++) {
            Log.i(TAG, String.format("key = %s, value = %s, docID = %s",
                    new String(e.key().readString().getBuf()),
                    new String(e.value().readString().getBuf()),
                    new String(e.docID().getBuf())));
        }
        assertEquals(6, _rowCount.intValue());
        assertEquals(6, nRows);

        Log.i(TAG, "--- Enumerating a vector of keys");
        VectorKeyRange keys = new VectorKeyRange();
        keys.add(new KeyRange(new Collatable("Cambria")));
        keys.add(new KeyRange(new Collatable("San Jose")));
        keys.add(new KeyRange(new Collatable("Portland")));
        keys.add(new KeyRange(new Collatable("Skookumchuk")));

        nRows = 0;
        e = new IndexEnumerator(index, keys, new DocEnumerator.Options());
        for (; e.next(); nRows++) {
            Log.i(TAG, "key => " + new String(e.key().readString().getBuf()) + ", docID => " + new String(e.docID().getBuf()));
        }
        assertEquals(2, nRows);

        Log.i(TAG, "--- Enumerating a vector of key ranges");
        VectorKeyRange ranges = new VectorKeyRange();
        ranges.add(new KeyRange(new Collatable("Port"), new Collatable("Port\uFFFE")));
        ranges.add(new KeyRange(new Collatable("Vernon"), new Collatable("Ypsilanti")));

        nRows = 0;
        e = new IndexEnumerator(index, ranges, new DocEnumerator.Options());
        for (; e.next(); nRows++) {
            Log.i(TAG, "key => " + new String(e.key().readString().getBuf()) + ", docID => " + new String(e.docID().getBuf()));
        }
        assertEquals(3, nRows);
    }

    public void testDuplicateKeys() throws Exception {
        Log.i(TAG, "--- Populate index");
        {
            Transaction trans = new Transaction(db);
            IndexWriter writer = new IndexWriter(index, trans);
            VectorCollatable keys = new VectorCollatable();
            VectorCollatable values = new VectorCollatable();
            Collatable key = new Collatable("Schlage");
            keys.add(key);
            values.add(new Collatable("purple"));
            keys.add(key);
            values.add(new Collatable("red"));
            assertTrue(writer.update(new Slice("doc1".getBytes()), BigInteger.valueOf(1), keys, values));
            _rowCount = writer.getRowCount();
            assertEquals(2, _rowCount.intValue());
            writer.delete();
            trans.delete();
        }

        Log.i(TAG, "--- First query");
        assertEquals(2, doQuery());

        {
            Transaction trans = new Transaction(db);
            IndexWriter writer = new IndexWriter(index, trans);
            VectorCollatable keys = new VectorCollatable();
            VectorCollatable values = new VectorCollatable();
            Collatable key = new Collatable("Schlage");
            keys.add(key);
            values.add(new Collatable("purple"));
            keys.add(key);
            values.add(new Collatable("crimson"));
            keys.add(new Collatable("Master"));
            values.add(new Collatable("gray"));
            writer.setRowCount(_rowCount);
            assertTrue(writer.update(new Slice("doc1".getBytes()), BigInteger.valueOf(2), keys, values));
            _rowCount = writer.getRowCount();
            assertEquals(3, _rowCount.intValue());
            writer.delete();
            trans.delete();
        }

        Log.i(TAG, "--- Second query");
        assertEquals(3, doQuery());
    }
}
