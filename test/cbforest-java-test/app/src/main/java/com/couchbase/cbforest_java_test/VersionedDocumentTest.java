package com.couchbase.cbforest_java_test;

import android.app.Activity;
import android.util.Log;

import com.couchbase.lite.cbforest.Database;
import com.couchbase.lite.cbforest.RevID;
import com.couchbase.lite.cbforest.RevIDBuffer;
import com.couchbase.lite.cbforest.RevTree;
import com.couchbase.lite.cbforest.Revision;
import com.couchbase.lite.cbforest.Slice;
import com.couchbase.lite.cbforest.Transaction;
import com.couchbase.lite.cbforest.VectorRevision;
import com.couchbase.lite.cbforest.VersionedDocument;

import java.io.File;
import java.math.BigInteger;

public class VersionedDocumentTest {
	public static final String TAG = "VersionedDocumentTest";
    public static final String dbfilename = "forest_temp.fdb";

    private Activity activity = null;
    Database db = null;

    public VersionedDocumentTest(Activity activity){
        this.activity = activity;
    }
    
    public void test() throws Exception {
        //test00_RevIDBuffer();
    	//test01_Empty();
    	//test02_RevTreeInsert();
    	//test03_AddRevision();
        //test04_DocType();
        testAll();
	}

    void testAll() throws Exception {
        test00_RevIDBuffer();
    	test01_Empty();
    	test02_RevTreeInsert();
    	test03_AddRevision();
        test04_DocType();
    }
    
    void setUp() throws Exception {
        File dbFile = new File(activity.getFilesDir(), dbfilename);
        Log.i(TAG, "dbFile=" + dbFile);
        if(dbFile.exists()){
            if(!dbFile.delete()){
                Log.e(TAG, "ERROR failed to delete: dbFile="+dbFile);
            }
        }
        db = new Database(dbFile.getPath(), Database.defaultConfig());
    }

    void tearDown(){
        db.delete();
    }

    void test00_RevIDBuffer() throws Exception {
        Log.i(TAG, "[test00_RevIDBuffer()] START");
        setUp();

        Slice s= new Slice("1-aa47cdab108f527c479ca6c3c8ee2869".getBytes());
        Log.i(TAG, "slice => " + new String(s.getBuf()) + ", size => " + s.getSize());
        RevIDBuffer buff = new RevIDBuffer();
        buff.parse(s);
        Slice exp = buff.expanded();
        Log.i(TAG, "exp.getSize() => " + exp.getSize());
        Log.i(TAG, "exp.getBuf() => " + new String(exp.getBuf()));

        Log.i(TAG, "expandedSize() => " + buff.expandedSize()); // 34
        Log.i(TAG, "length => " + buff.getBuf().length);        // 17
        Log.i(TAG, new String(buff.getBuf()));                  // 17 chars...
        Log.i(TAG, buff.toString());

        tearDown();
        Log.i(TAG, "[test00_RevIDBuffer()] END");
    }
	void test01_Empty() throws Exception {
		Log.i(TAG, "[test01_Empty()] START");
		setUp();
		VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));
		if (!new String(v.getDocID().getBuf()).equals("foo"))
			Log.e(TAG, "v.getDocID().getBuf()=" + v.getDocID().getBuf());
		Log.i(TAG, "v.getRevID()=" + v.getRevID());
		Log.i(TAG, "v.getRevID()=" + v.getRevID().getBuf());
		if (v.getRevID().getBuf() != null)
			Log.e(TAG, "v.getRevID().getBuf()=" + new String(v.getRevID().getBuf()));
		if (v.getFlags() != 0)
			Log.e(TAG, "v.getFlags()=" + v.getFlags());
		Revision rev = v.get(new RevIDBuffer(new Slice("1-aaaa".getBytes())));
		if (rev != null) {
			Log.e(TAG, "rev=" + rev);
			rev.delete();
		}
		v.delete();
		tearDown();
		Log.i(TAG, "[test01_Empty()] END");
	}

	// CouchBase Lite does not call RevTree directly. Only through VersionedDocument
    void test02_RevTreeInsert() throws Exception {
		Log.i(TAG, "[test02_RevTreeInsert()] START");
    	setUp();

		RevTree tree = new RevTree();
		RevIDBuffer rev1ID = new RevIDBuffer(new Slice("1-aaaa".getBytes()));
		Slice rev1Data = new Slice("body of revision".getBytes());
		Revision rev = tree.insert(rev1ID, rev1Data, false, false, new RevID(), false);
		if (rev == null)
			Log.e(TAG, "rev=" + rev);
		if (tree.getLatestHttpStatus() != 201)
			Log.e(TAG, "httpStatus=" + tree.getLatestHttpStatus());
		if (rev.getRevID().compare(rev1ID) != 0)
			Log.e(TAG, "rev.getRevID()!=rev1ID");
		if (rev.getParent() != null)
			Log.e(TAG, "(rev.getParent()!=null");
		if (rev.isDeleted() == true)
			Log.e(TAG, "rev.isDeleted()==true");
		Log.i(TAG, "rev.getRevID() => " + rev.getRevID());
    	//Log.i(TAG, "rev.getRevID() => " + new String(rev.getRevID().getBuf()));

		RevIDBuffer rev2ID = new RevIDBuffer(new Slice("2-bbbb".getBytes()));
		Slice rev2Data = new Slice("second revision".getBytes());
		Revision rev2 = tree.insert(rev2ID, rev2Data, false, false, rev1ID, false);
		if (rev2 == null)
			Log.e(TAG, "rev2=" + rev2);
		if (tree.getLatestHttpStatus() != 201)
			Log.e(TAG, "httpStatus=" + tree.getLatestHttpStatus());
		if (rev2.getRevID().compare(rev2ID) != 0)
			Log.e(TAG, "rev2.getRevID()!=rev2ID");
		if (rev2.isDeleted() == true)
			Log.e(TAG, "rev2.isDeleted()==true");
		Log.i(TAG, "rev2.getRevID() => " + rev2.getRevID());
		//Log.i(TAG, "rev2.getRevID() => " + new String(rev2.getRevID().getBuf()));

		tree.sort();

        rev = tree.get(rev1ID);
        rev2 = tree.get(rev2ID);
        if(rev==null)
            Log.e(TAG, "rev=" + rev);
        if(rev2==null)
            Log.e(TAG, "rev2=" + rev2);
        if(rev2.getParent() == null)
            Log.e(TAG, "rev2.getParent()==null");
        if(rev.getParent() != null)
            Log.e(TAG, "rev.getParent()!=null");
        if(!rev2.getParent().isSameAddress(rev))
            Log.e(TAG, "rev2.getParent()!=rev");
        if(rev2.getParent().getRevID().compare(rev.getRevID())!=0)
            Log.e(TAG, "rev2.getParent().getRevID()!=rev.getRevID()");

        if(!tree.currentRevision().isSameAddress(rev2))
            Log.e(TAG, "!tree.currentRevision().isSameAddress(rev2)");
        if(tree.hasConflict())
            Log.e(TAG, "tree.hasConflict()");

        tree.sort();

        if(!tree.get(0).isSameAddress(rev2))
            Log.e(TAG, "!tree.get(0).isSameAddress(rev2)");
        if(!tree.get(1).isSameAddress(rev))
            Log.e(TAG, "!tree.get(1).isSameAddress(rev)");
        if (rev.index() != 1)
            Log.e(TAG, "rev.index() should be 1. rev.index() => " + rev.index());
        if (rev2.index() != 0)
            Log.e(TAG, "rev2.index() should be 0. rev2.index() => " + rev2.index());

    	
    	Slice ext = tree.encode();
		RevTree tree2 = new RevTree(ext, BigInteger.valueOf(12), BigInteger.valueOf(1234));
    	
    	tearDown();
		Log.i(TAG, "[test02_RevTreeInsert()] END");
    }

    void test03_AddRevision() throws Exception {
        Log.i(TAG, "[test03_AddRevision()] START");
    	setUp();  
    	
    	String revID = "1-fadebead";
    	String body = "{\"hello\":true}";
    	RevIDBuffer revIDBuf = new RevIDBuffer(new Slice(revID.getBytes()));
    	VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));
    	v.insert(revIDBuf, new Slice(body.getBytes()), false, false, (Revision)null, false);
    	if(v.getLatestHttpStatus()!=201)
    		Log.e(TAG, "v.getLatestHttpStatus()=" + v.getLatestHttpStatus());
    	Revision node = v.get(new RevIDBuffer(new Slice(revID.getBytes())));
    	if(node == null)
    		Log.e(TAG, "node=" + node);
    	if(node.isDeleted())
    		Log.e(TAG, "node.isDeleted()=" + node.isDeleted());
    	if(!node.isLeaf())
    		Log.e(TAG, "node.isLeaf()=" + node.isLeaf());
    	if(!node.isActive())
    		Log.e(TAG, "node.isActive()=" + node.isActive());
    	if(v.size()!=1)
    		Log.e(TAG, "v.size()=" + v.size());
    	VectorRevision revs = v.currentRevisions();
    	if(revs.size()!=1)
    		Log.e(TAG, "revs.size()=" + revs.size());
    	Revision rev1 = revs.get(0);
    	Revision rev2 = v.currentRevision();
    	if(!rev1.isSameAddress(rev2))
    		Log.e(TAG, "rev1!=rev2");

    	tearDown();
        Log.i(TAG, "[test03_AddRevision()] END");
    }

    void test04_DocType() throws Exception {
        Log.i(TAG, "[test04_DocType()] START");
        setUp();

		RevIDBuffer rev1ID = new RevIDBuffer(new Slice("1-aaaa".getBytes()));

		{
			VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));

			Slice rev1Data = new Slice("body of revision".getBytes());

			v.insert(rev1ID, rev1Data, true/*deleted*/, false, new RevID(), false);
			int httpStatus = v.getLatestHttpStatus();
			v.setDocType(new Slice("moose".getBytes()));
			if(!"moose".equals(new String(v.getDocType().getBuf())))
				Log.e(TAG, "v.getDocType() should be `moose`.");
			Transaction t =  new Transaction(db);
			v.save(t);
			t.delete();
		}

		{
			VersionedDocument v = new VersionedDocument(db, new Slice("foo".getBytes()));
			if(v.getFlags()!=VersionedDocument.kDeleted)
				Log.e(TAG, "v.getFlags() should be VersionedDocument.kDeleted");
			if (v.getRevID().compare(rev1ID) != 0)
				Log.e(TAG, "v.getRevID()!=rev1ID");
			if(!"moose".equals(new String(v.getDocType().getBuf())))
				Log.e(TAG, "v.getDocType() should be `moose`.");
		}

        tearDown();
        Log.i(TAG, "[test04_DocType()] END");
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
