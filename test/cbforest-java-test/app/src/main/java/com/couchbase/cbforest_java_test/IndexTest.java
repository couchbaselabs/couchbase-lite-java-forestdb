package com.couchbase.cbforest_java_test;

import android.app.Activity;
import android.util.Log;

import com.couchbase.lite.cbforest.Collatable;
import com.couchbase.lite.cbforest.CollatableReader;
import com.couchbase.lite.cbforest.Database;
import com.couchbase.lite.cbforest.DocEnumerator;
import com.couchbase.lite.cbforest.Index;
import com.couchbase.lite.cbforest.IndexEnumerator;
import com.couchbase.lite.cbforest.IndexWriter;
import com.couchbase.lite.cbforest.KeyRange;
import com.couchbase.lite.cbforest.Slice;
import com.couchbase.lite.cbforest.Transaction;
import com.couchbase.lite.cbforest.VectorCollatable;
import com.couchbase.lite.cbforest.VectorKeyRange;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexTest {
    public static final String TAG = "IndexTest";

	public static final String dbfilename = "forest_temp.fdb";
	 
    private Activity activity = null;
    Database db = null;
    Index index = null;
    BigInteger _rowCount;
    
    public IndexTest(Activity activity){
        this.activity = activity;
    }

    public void test() throws Exception {
    	//testBasics();
        //testDuplicateKeys();
    	testAll();
    }
    
    void testAll() throws Exception {
    	testBasics();
        testDuplicateKeys();
    }

    void setUp() throws Exception {
    	File dbFile = new File(activity.getFilesDir(), dbfilename);
        Log.i(TAG, "dbFile="+dbFile);
        if(dbFile.exists()){
            if(!dbFile.delete()){
                Log.e(TAG, "ERROR failed to delete: dbFile="+dbFile);
            }
        }
        db = new Database(dbFile.getPath(), Database.defaultConfig());
        index = new Index(db, "index");
    }

    void tearDown(){
    	if(index != null){
    		index.delete();
    		index = null;
    	}
    	if(db!=null){
    		db.delete();
    		db = null;
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
    	boolean changed = writer.update(new Slice(docID.getBytes()), BigInteger.valueOf(1), keys, values);
    	if(!changed)
    		Log.e("IndexTest", "ERROR: Failed to load libcbforest !!!");
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
        if(nRows != _rowCount.intValue())
            Log.e(TAG, String.format("nRows should be equal to _rowCount. nRows => %d, _rowCount => %d", nRows, _rowCount.intValue()));
        return nRows;
    }

    void testBasics() throws Exception {
        Log.i(TAG, "[testBasics()] START");
        setUp();

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
            writer = null;
            trans.delete();
            trans = null;
        }

        Log.i(TAG, "--- First query");
        if (doQuery() != 8)
            Log.e(TAG, "nRows != 8");
        {
            Log.i(TAG, "--- Updating OR");
            Transaction trans = new Transaction(db);
            IndexWriter writer = new IndexWriter(index, trans);
            writer.setRowCount(_rowCount);
            updateDoc("OR", Arrays.asList("Oregon", "Portland", "Walla Walla", "Salem"), writer);
            _rowCount = writer.getRowCount();
            writer.delete();
            writer = null;
            trans.delete();
            trans = null;
        }
        if (doQuery() != 9)
            Log.e(TAG, "nRows != 9");

        {
            Log.i(TAG, "--- Removing CA");
            Transaction trans = new Transaction(db);
            IndexWriter writer = new IndexWriter(index, trans);
            writer.setRowCount(_rowCount);
            updateDoc("CA", new ArrayList<String>(), writer);
            _rowCount = writer.getRowCount();
            writer.delete();
            writer = null;
            trans.delete();
            trans = null;
        }
        if (doQuery() != 6)
            Log.e(TAG, "nRows != 6");

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
        if(nRows != _rowCount.intValue())
            Log.e(TAG, String.format("nRows should be equal to _rowCount. nRows => %d, _rowCount => %d", nRows, _rowCount.intValue()));
        if(nRows != 6)
            Log.e(TAG, "nRows != 6");

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
        if (nRows != 2)
            Log.e(TAG, "nRows != 2");

        Log.i(TAG, "--- Enumerating a vector of key ranges");
        VectorKeyRange ranges = new VectorKeyRange();
        ranges.add(new KeyRange(new Collatable("Port"), new Collatable("Port\uFFFE")));
        ranges.add(new KeyRange(new Collatable("Vernon"), new Collatable("Ypsilanti")));

        nRows = 0;
        e = new IndexEnumerator(index, ranges, new DocEnumerator.Options());
        for (; e.next(); nRows++) {
            Log.i(TAG, "key => " + new String(e.key().readString().getBuf()) + ", docID => " + new String(e.docID().getBuf()));
        }
        if (nRows != 3)
            Log.e(TAG, "nRows != 3");

        tearDown();
        Log.i(TAG, "[testBasics()] END");
    }

    void testDuplicateKeys() throws Exception {
        Log.i(TAG, "[testDuplicateKeys()] START");
        setUp();
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
            boolean changed = writer.update(new Slice("doc1".getBytes()), BigInteger.valueOf(1), keys, values);
            if(!changed)
                Log.e("IndexTest", "ERROR: Failed to IndexWriter.update() !!!");
            _rowCount = writer.getRowCount();
            if(_rowCount.intValue() != 2)
                Log.e("IndexTest", "ERROR: _rowCount should be 2. _rowCount => "+ _rowCount);
            writer.delete();
            trans.delete();
        }

        Log.i(TAG, "--- First query");
        if (doQuery() != 2)
            Log.e(TAG, "nRows != 2");

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
            boolean changed = writer.update(new Slice("doc1".getBytes()), BigInteger.valueOf(2), keys, values);
            if(!changed)
                Log.e("IndexTest", "ERROR: Failed to IndexWriter.update() !!!");
            _rowCount = writer.getRowCount();
            if(_rowCount.intValue() != 3)
                Log.e("IndexTest", "ERROR: _rowCount should be 3. _rowCount => "+ _rowCount);
            writer.delete();
            trans.delete();
        }

        Log.i(TAG, "--- Second query");
        if (doQuery() != 3)
            Log.e(TAG, "nRows != 3");

        tearDown();
        Log.i(TAG, "[testDuplicateKeys()] END");
    }

    void testBlockScopedObjects() throws Exception {
        // Not applicable to Java ??
    }

    /** static constructor */
    static {
        try{
            Log.i(TAG, "load libcbforest start");
            System.loadLibrary("cbforest");
            Log.i(TAG, "load libcbforest OK !!!");
        }
        catch(Exception e){
            Log.e(TAG, "ERROR: Failed to load libcbforest !!!");
        }
    }
}
