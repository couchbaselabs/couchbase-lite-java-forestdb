package com.couchbase.cbforest_java_test;

import android.app.Activity;
import android.util.Log;

import com.couchbase.lite.cbforest.Config;
import com.couchbase.lite.cbforest.ContentOptions;
import com.couchbase.lite.cbforest.Database;
import com.couchbase.lite.cbforest.DocEnumerator;
import com.couchbase.lite.cbforest.Document;
import com.couchbase.lite.cbforest.FileInfo;
import com.couchbase.lite.cbforest.OpenFlags;
import com.couchbase.lite.cbforest.Slice;
import com.couchbase.lite.cbforest.Transaction;
import com.couchbase.lite.cbforest.VectorString;

import java.io.File;
import java.math.BigInteger;

public class DatabaseTest {
    public static final String TAG = DatabaseTest.class.getSimpleName();

    public static final String dbfilename = "forest_temp.fdb";

    private Activity activity = null;
    Database db = null;

    public DatabaseTest(Activity activity){
        this.activity = activity;
    }

    public void test() throws Exception {
    	//test00_Slice();
        //test00_Document();
        //test01_DbInfo();
        //test02_CreateDoc();
        //test03_SaveDocs();
        //test04_EnumerateDocs();
        //test05_AbortTransaction();
        //test06_TransactionsThenIterate();
        //this.test07_DeleteDoc();
        //this.test08_KeyStoreInfo();
        //this.test09_KeyStoreWrite();
        //this.test10_KeyStoreDelete();
        //this.test11_ReadOnly();
    	testAll();
    }
    void testAll()throws Exception{
        test00_Slice();
        test00_Document();
        test01_DbInfo();
        test02_CreateDoc();
        test03_SaveDocs();
        test04_EnumerateDocs();
        test05_AbortTransaction();
        test06_TransactionsThenIterate();
        this.test07_DeleteDoc();
        this.test08_KeyStoreInfo();
        this.test09_KeyStoreWrite();
        this.test10_KeyStoreDelete();
        this.test11_ReadOnly();
    }

    void setUp() throws Exception {
        File dbFile = new File(activity.getFilesDir(), dbfilename);
        //Log.i(TAG, "dbFile=" + dbFile);
        if(dbFile.exists()){
            if(!dbFile.delete()){
                Log.e(TAG, "ERROR failed to delete: dbFile="+dbFile);
            }
        }
        //Log.i(TAG, "[setUp()] call Database()");
        db = new Database(dbFile.getPath(), Database.defaultConfig());
        //Log.i(TAG, "[setUp()] db => " + db);
    }

    void tearDown(){
        db.delete();
    }

    void test00_Slice(){
        Log.i(TAG, "[test00_Slice()] START");
        
        String str = "こんにちは ForestDB!!!";
        
        Slice s= new Slice(str.getBytes());
        Log.i(TAG, "s.getBuf()=" + new String(s.getBuf()));
        String sliceStr = new String(s.getBuf());
        if(!sliceStr.equals(str))
        	Log.e(TAG, "s.getBuf()=" +  new String(s.getBuf()));
        if(s.getSize() != str.getBytes().length)
        	Log.e(TAG, "s.getSize()=" + s.getSize());
        s.delete();
        s = null;
        
        // default constructor
        s  = new Slice();
        if(s.getBuf() != null)
        	Log.e(TAG, "s.getBuf()=" + s.getBuf());
        if(s.getSize() != 0)
        	Log.e(TAG, "s.getSize()=" + s.getSize());
        s.delete();
        s = null;

        Log.i(TAG, "[test00_Slice()] END");
    }
    void test00_Document(){
        Log.i(TAG, "[test00_Document()] START");

        Document doc = new Document(new Slice("key".getBytes()));
        if(!new String(doc.getKey().getBuf()).equals("key"))
            Log.e(TAG, "doc.getKey().getBuf()=" +  new String(doc.getKey().getBuf()));
        doc.setKey(new Slice("ABCDE".getBytes()));
        doc.setMeta(new Slice("abcde".getBytes()));
        doc.setBody(new Slice("12345".getBytes()));
        if(!new String(doc.getKey().getBuf()).equals("ABCDE"))
            Log.e(TAG, "doc.getKey().getBuf()=" +  new String(doc.getKey().getBuf()));
        if(!new String(doc.getMeta().getBuf()).equals("abcde"))
            Log.e(TAG, "doc.getMeta().getBuf()=" +  new String(doc.getMeta().getBuf()));
        if(!new String(doc.getBody().getBuf()).equals("12345"))
            Log.e(TAG, "doc.getBody().getBuf()=" +  new String(doc.getBody().getBuf()));
        doc.delete();

        doc = new Document();
        Log.i(TAG, "doc.getKey().getBuf()=" + doc.getKey().getBuf());
        Log.i(TAG, "doc.getMeta().getBuf()=" + doc.getMeta().getBuf());
        Log.i(TAG, "doc.getBody().getBuf()=" + doc.getBody().getBuf());

        Log.i(TAG, "[test00_Document()] END");
    }
    void test01_DbInfo() throws Exception {
        Log.i(TAG, "[test01_DbInfo()] START");
        setUp();
        
        FileInfo info = db.getFileInfo();
        if(info.getDocCount().intValue() != 0)
        	Log.e(TAG, "info.getDocCount(): " + info.getDocCount());
        if(info.getSpaceUsed().intValue() != 0)
        	Log.e(TAG, "info.getSpaceUsed(): " + info.getSpaceUsed());
        if(info.getFileSize().intValue() <= 0)
        	Log.e(TAG, "info.getFileSize(): " + info.getFileSize());
        Log.i(TAG, "filename => " + db.getFilename());

        if(db.getLastSequence().intValue() != 0)
            Log.e(TAG, "db.getLastSequence(): " + db.getLastSequence());
        tearDown();
        Log.i(TAG, "[test01_DbInfo()] END");
    }

    void test02_CreateDoc() throws Exception {
        Log.i(TAG, "[test02_CreateDoc()] START");
        setUp();

        // create doc
        Slice key = new Slice("key".getBytes());
        {
        	Transaction t = new Transaction(db);
        	t.set(key, new Slice("value".getBytes()));
            t.delete();
        }
        
        if(db.getLastSequence().intValue() != 1)
        	Log.e(TAG, "ERROR: lastSequence: " + db.getLastSequence());
        Document doc = db.get(key);
        if(doc.getKey().compare(key) != 0)
            Log.e(TAG, "ERROR: key: " + doc.getKey().getBuf());
        if(doc.getBody().compare(new Slice("value".getBytes())) != 0)
            Log.e(TAG, "ERROR: body: " + doc.getBody().getBuf());
        doc.delete();

        key.delete();
        
        tearDown();
        Log.i(TAG, "[test02_CreateDoc()] END");
    }

    void test03_SaveDocs() throws Exception {
        Log.i(TAG, "[test03_SaveDocs()] START");

        setUp();
        
        Document tmp_doc = null;

        //WORKAROUND: Add a doc before the main transaction so it doesn't start at sequence 0
        Transaction tmp_t = new Transaction(db);
        tmp_t.set(new Slice("a".getBytes()), new Slice("A".getBytes()));
	   	tmp_t.delete();
	   	tmp_t = null;
      
        // test with another database instance which points to same database file
        File dbFile = new File(activity.getFilesDir(), dbfilename);
        Database aliased_db = new Database(dbFile.getPath(), Database.defaultConfig());
        
        tmp_doc = aliased_db.get(new Slice("a".getBytes()));
        if(tmp_doc != null) {
            if (tmp_doc.getKey().compare(new Slice("a".getBytes())) != 0)
            	Log.e(TAG, "tmp_doc.getKey() should be 'a'. but tmp_doc.getKey()='" + new String(tmp_doc.getKey().getBuf()) + "'");
            if (tmp_doc.getBody().compare(new Slice("A".getBytes())) != 0)
            	Log.e(TAG, "tmp_doc.getBody() should be 'A'. but tmp_doc.getBody()='" + new String(tmp_doc.getBody().getBuf()) + "'");
        }
        else{
            Log.e(TAG, "aliasDb.get(new Slice(\"a\") should not be null!!!!");
        }
        tmp_doc.delete();
        
        {
	        Transaction t = new Transaction(db);
	        Document doc = new Document(new Slice("doc".getBytes()));
	        doc.setMeta(new Slice("m-e-t-a".getBytes()));
	        doc.setBody(new Slice("THIS IS THE BODY".getBytes()));
	        t.write(doc);
	        
	        if(!doc.getSequence().equals(BigInteger.valueOf(2)))
	            Log.e(TAG, "sequence should be 2. but doc.sequence=" + doc.getSequence());
	        if(db.getLastSequence().intValue() != 2)
	        	Log.e(TAG, "ERROR: lastSequence: " + db.getLastSequence());
	        
	        Document doc_alias = t.get(doc.getSequence());
	        if(doc_alias != null) {
	            if (!new String(doc_alias.getKey().getBuf()).equals(new String(doc.getKey().getBuf())))
	            	Log.e(TAG, "aliasDoc2.getKey() should be equal with doc.getKey(). but aliasDoc2.getKey()='" + new String(doc_alias.getKey().getBuf()) + "'");
	            if (!new String(doc_alias.getMeta().getBuf()).equals(new String(doc.getMeta().getBuf())))
	            	Log.e(TAG, "aliasDoc2.getMeta() should be equal with doc.getMeta(). but aliasDoc2.getMeta()='" + new String(doc_alias.getMeta().getBuf()) + "'");
	            if (!new String(doc_alias.getBody().getBuf()).equals(new String(doc.getBody().getBuf())))
	            	Log.e(TAG, "aliasDoc2.getBody() should be equal with doc.getBody().. but aliasDoc2.getBody()='" + new String(doc_alias.getBody().getBuf()) + "'");
	        }
	        else{
	            Log.e(TAG, "t.get(doc.getSequence()) should not be null!!!!");
	        }
	        
	        doc_alias.setBody(new Slice("NU BODY".getBytes()));
	        t.write(doc_alias);
	        
	        if(t.read(doc) == false)
	        	Log.e(TAG, "t2.read(doc) should return true... but result=false");
	        if(!doc.getSequence().equals(BigInteger.valueOf(3)))
	            Log.e(TAG, "sequence should be 3. but doc.sequence=" + doc_alias.getSequence());
            if (!new String(doc.getMeta().getBuf()).equals(new String(doc_alias.getMeta().getBuf())))
            	Log.e(TAG, "aliasDoc2.getMeta() should be equal with doc.getMeta(). but aliasDoc2.getMeta()='" + new String(doc_alias.getMeta().getBuf()) + "'");
            if (!new String(doc.getBody().getBuf()).equals(new String(doc_alias.getBody().getBuf())))
            	Log.e(TAG, "aliasDoc2.getBody() should be equal with doc.getBody().. but aliasDoc2.getBody()='" + new String(doc_alias.getBody().getBuf()) + "'");
	        
	        doc_alias.delete();
	        doc_alias = null;
	        
	        // Doc shouldn't exist outside transaction yet:
	        tmp_doc = aliased_db.get(new Slice("doc".getBytes()));
	        if(tmp_doc.getSequence().intValue() != 0)
	        	Log.e(TAG, "tmp_doc.getSequence() should not be 0. But it is " + tmp_doc.getSequence());
	        tmp_doc.delete();
	        
	        // Release Transaction 2
	        t.delete();
	        t = null;
	   }
        
	   tmp_doc = db.get(new Slice("doc".getBytes()));
	   if(tmp_doc.getSequence().intValue() != 3)
		   Log.e(TAG, "tmp_doc.getSequence() should be 3. But it is " + tmp_doc.getSequence());
	   tmp_doc.delete();
	   
	   tmp_doc = aliased_db.get(new Slice("doc".getBytes()));
	   if(tmp_doc.getSequence().intValue() != 3)
		   Log.e(TAG, "tmp_doc.getSequence() should be 3. But it is " + tmp_doc.getSequence());
	   tmp_doc.delete();
	   
	   aliased_db.delete();
	   aliased_db = null;
        
        tearDown();
        Log.i(TAG, "[test03_SaveDocs()] END");
    }

    void createNumberedDocs() throws Exception {
    	Transaction t = new Transaction(db);
        for (int i = 1; i <= 100; i++) {
        	String docID = String.format("doc-%03d", i);
            BigInteger seq = t.set(new Slice(docID.getBytes()), new Slice(), new Slice(docID.getBytes()));
            if(seq.intValue() != i)
            	Log.e(TAG, "seq should be "+ i +". But it is " + seq);
            if(!new String(t.get(new Slice(docID.getBytes())).getBody().getBuf()).equals(docID))
            	Log.e(TAG, "t.get(new Slice(docID)).getBody() is " + new String(t.get(new Slice(docID.getBytes())).getBody().getBuf()));
        }
    	t.delete();
    }
    
    void test04_EnumerateDocs() throws Exception {
        Log.i(TAG, "[test04_EnumerateDocs()] START");
        setUp();

        {
            Log.i(TAG, "Enumerate empty db");
            DocEnumerator e = new DocEnumerator(db, new Slice(), new Slice(), new DocEnumerator.Options());
            for(; e.next();){
                Log.e(TAG, "Shouldn't have found any docs");
            }
            e.delete();
        }

        createNumberedDocs();

        for(int metaOnly = 0; metaOnly <= 1; ++metaOnly) {

            Log.i(TAG, "Enumerate over all docs: metaOnly=" + metaOnly);

            DocEnumerator.Options opts = new DocEnumerator.Options();
            opts.setContentOption(metaOnly == 1 ? ContentOptions.kMetaOnly:ContentOptions.kDefaultContent);

            int i = 1;
            DocEnumerator e = new DocEnumerator(db, new Slice(), new Slice(), opts);
            for (; e.next(); ++i) {
                Document doc = e.doc();
                if (doc == null) {
                    Log.e(TAG, "ERROR: doc is null");
                } else {
                    String docID = String.format("doc-%03d", i);
                    if (!new String(doc.getKey().getBuf()).equals(docID))
                        Log.e(TAG, "doc.getKey() should be " + docID + ". but " + new String(doc.getKey().getBuf()));
                    if (doc.getSequence().intValue() != i)
                        Log.e(TAG, "doc.getSequence() should be " + i + ". but " + doc.getSequence());
                    if(doc.getBody().getSize() <= 0)
                        Log.e(TAG, "doc.getBody().getSize() should be > 0. but " + doc.getBody().getSize());
                    if(e.doc().offset().intValue() <= 0)
                        Log.e(TAG, "e.doc().offset() should be > 0. but " + e.doc().offset());
                }
            }
            if (i != 101)
                Log.e(TAG, "i should be 101. but i=" + i);
            e.delete();
            e = null;

            Log.i(TAG, "Enumerate over range of docs:");
            i = 24;
            e = new DocEnumerator(db, new Slice("doc-024".getBytes()), new Slice("doc-029".getBytes()), opts);
            for (; e.next(); ++i) {
                Document doc = e.doc();
                String docID = String.format("doc-%03d", i);
                if (!new String(doc.getKey().getBuf()).equals(docID))
                    Log.e(TAG, "doc.getKey() should be " + docID + ". but " + doc.getKey().getBuf());
                if (doc.getSequence().intValue() != i)
                    Log.e(TAG, "doc.getSequence() should be " + i + ". but " + doc.getSequence());
                if(doc.getBody().getSize() <= 0)
                    Log.e(TAG, "doc.getBody().getSize() should be > 0. but " + doc.getBody().getSize());
                if(e.doc().offset().intValue() <= 0)
                    Log.e(TAG, "e.doc().offset() should be > 0. but " + e.doc().offset());
            }
            if (i != 30)
                Log.e(TAG, "i should be 30. but i=" + i);
            e.delete();
            e = null;

            Log.i(TAG, "Enumerate over range of docs without inclusive:");
            //DocEnumerator.Options options = DocEnumerator.Options.getDef();
            opts.setInclusiveStart(false);
            opts.setInclusiveEnd(false);
            i = 25;
            e = new DocEnumerator(db, new Slice("doc-024".getBytes()), new Slice("doc-029".getBytes()), opts);
            for (; e.next(); ++i) {
                Document doc = e.doc();
                String docID = String.format("doc-%03d", i);
                if (!new String(doc.getKey().getBuf()).equals(docID))
                    Log.e(TAG, "doc.getKey() should be " + docID + ". but " + new String(doc.getKey().getBuf()));
                if (doc.getSequence().intValue() != i)
                    Log.e(TAG, "doc.getSequence() should be " + i + ". but " + doc.getSequence());
                if(doc.getBody().getSize() <= 0)
                    Log.e(TAG, "doc.getBody().getSize() should be > 0. but " + doc.getBody().getSize());
                if(e.doc().offset().intValue() <= 0)
                    Log.e(TAG, "e.doc().offset() should be > 0. but " + e.doc().offset());
            }
            if (i != 29)
                Log.e(TAG, "i should be 29. but i=" + i);
            e.delete();
            e = null;


            Log.i(TAG, "Enumerate over vector of docs:");
            i = 0;
            VectorString docIDs = new VectorString();
            docIDs.add("doc-005");
            docIDs.add("doc-023");
            docIDs.add("doc-028");
            docIDs.add("doc-029");
            docIDs.add("doc-098");
            docIDs.add("doc-100");
            docIDs.add("doc-105");
            e = new DocEnumerator(db, docIDs, opts);
            for (; e.next(); ++i) {
                Document doc = e.doc();
                if (doc.getKey().getBuf() != null && doc.getBody().getBuf() != null)
                    Log.i(TAG, "doc.getKey()=" + new String(doc.getKey().getBuf()) + " body=" + new String(doc.getBody().getBuf()));
                else if (doc.getKey().getBuf() != null && doc.getBody().getBuf() == null)
                    Log.i(TAG, "doc.getKey()=" + new String(doc.getKey().getBuf()) + " body=" + doc.getBody().getBuf());
                else
                    Log.i(TAG, "doc.getKey()=" + doc.getKey().getBuf() + " body=" + doc.getBody().getBuf());

                if (!new String(doc.getKey().getBuf()).equals(docIDs.get(i)))
                    Log.e(TAG, "doc.getKey() should be " + docIDs.get(i) + ". but " + new String(doc.getKey().getBuf()));
                if(e.doc().exists() != (i < 6))
                    Log.e(TAG, "Doc shold exist if i < 6, not exist i >= 7. i = " + i);
                if(i < 6){
                    if(doc.getBody().getSize() <= 0)
                        Log.e(TAG, "doc.getBody().getSize() should be > 0. but " + doc.getBody().getSize());
                    if(e.doc().offset().intValue() <= 0)
                        Log.e(TAG, "e.doc().offset() should be > 0. but " + e.doc().offset());
                }
            }
            if (i != 7)
                Log.e(TAG, "i should be 7. but i=" + i);
            e.delete();
            e = null;
        }
        tearDown();
        Log.i(TAG, "[test04_EnumerateDocs()] END");
    }

    void test05_AbortTransaction() throws Exception {
        Log.i(TAG, "[test05_AbortTransaction()] START");
        setUp();

        // Initial document:
        Transaction tmpT = new Transaction(db);
        tmpT.set(new Slice("a".getBytes()), new Slice("A".getBytes()));
        tmpT.delete();
        tmpT = null;
        
        // main code
        {
			Transaction t = new Transaction(db);
			t.set(new Slice("x".getBytes()), new Slice("X".getBytes()));
			t.set(new Slice("a".getBytes()), new Slice("Z".getBytes()));
			if(!new String(t.get(new Slice("a".getBytes())).getBody().getBuf()).equals("Z"))
				Log.e(TAG, "t.get(new Slice(\"a\")).getBody().getBuf() should be 'Z'. But it is " + new String(t.get(new Slice("a".getBytes())).getBody().getBuf()));
			if(!new String(db.get(new Slice("a".getBytes())).getBody().getBuf()).equals("Z"))
				Log.e(TAG, "db.get(new Slice(\"a\")).getBody().getBuf() should be 'Z'. But it is " + new String(db.get(new Slice("a".getBytes())).getBody().getBuf()));
			t.abort();
			t.delete(); // <-- Need to call to clear
			t = null;
        }
		if(!new String(db.get(new Slice("a".getBytes())).getBody().getBuf()).equals("A"))
			Log.e(TAG, "db.get(new Slice(\"a\")).getBody().getBuf() should be 'A'. But it is " + db.get(new Slice("a".getBytes())).getBody().getBuf());
		if(db.get(new Slice("x".getBytes())).getSequence().intValue() != 0)
			Log.e(TAG, "db.get(new Slice(\"x\")).getSequence().intValue() should be 0. But it is " + db.get(new Slice("x".getBytes())).getSequence());
        
        tearDown();
        Log.i(TAG, "[test05_AbortTransaction()] END");
    }

    // Test for MB-12287
    void test06_TransactionsThenIterate() throws Exception {
        Log.i(TAG, "[test06_TransactionsThenIterate()] START");
        setUp();

        // test with another database instance which points to same database file
        File dbFile = new File(activity.getFilesDir(), dbfilename);
        Database db2 = new Database(dbFile.getPath(), Database.defaultConfig());

        final int kNTransactions = 41; // 41 is ok, 42+ fails
        final int kNDocs = 100;
        
        for(int t = 1; t <= kNTransactions; t++){
        	Transaction trans = new Transaction(db);
        	for(int d = 1; d <= kNDocs; d++){
        		String docID = String.format("%03d.%03d", t, d);
        		trans.set(new Slice(docID.getBytes()), new Slice("some document content goes here".getBytes()));
        	}
        	trans.delete(); // <--- DON'T FORGET!!!
        	trans = null;
        }
        
        int i = 0;
        DocEnumerator iter = new DocEnumerator(db2, new Slice(), new Slice(), new DocEnumerator.Options());
        for(; iter.next(); ++i){
            String key = new String(iter.doc().getKey().getBuf());
            int t = (i / kNDocs) + 1;
            int d = (i % kNDocs) + 1;
            String value = String.format("%03d.%03d", t, d);
            if(!key.equals(value))
                Log.e(TAG, "doc.key should be " + value + ". but it is " + key);
        }
        db2.delete();
        db2 = null;
        
        tearDown();
        Log.i(TAG, "[test06_TransactionsThenIterate()] END");
    }

    void test07_DeleteDoc() throws Exception {
        Log.i(TAG, "[test07_DeleteDoc()] START");
        setUp();

        Slice key = new Slice("a".getBytes());
        Transaction t1 = new Transaction(db);
        t1.set(key, new Slice("A".getBytes()));
        t1.delete();

        {
            Transaction t = new Transaction(db);
            Document doc = db.get(key);
            t.del(doc);
            doc.delete();
            t.delete();
        }

        Document doc = db.get(key);
        if(!doc.deleted())
            Log.e(TAG, "doc should be delted: " + doc.deleted());
        if(doc.exists())
            Log.e(TAG, "doc should not exist: " + doc.exists());
        doc.delete();

        key.delete();
        tearDown();
        Log.i(TAG, "[test07_DeleteDoc()] END");
    }
    void test08_KeyStoreInfo() throws Exception {
        Log.i(TAG, "[test08_KeyStoreInfo()] START");
        setUp();
        // TODO:
        // cbforest java does not allow to create KeyStore instance
        // KvsInfo is not implmented
        tearDown();
        Log.i(TAG, "[test08_KeyStoreInfo()] END");
    }
    void test09_KeyStoreWrite() throws Exception {
        Log.i(TAG, "[test09_KeyStoreWrite()] START");
        setUp();
        // TODO:
        // cbforest java does not allow to create KeyStore instance
        tearDown();
        Log.i(TAG, "[test09_KeyStoreWrite()] END");
    }
    void test10_KeyStoreDelete() throws Exception {
        Log.i(TAG, "[test10_KeyStoreDelete()] START");
        setUp();
        // TODO:
        // cbforest java does not allow to create KeyStore instance
        tearDown();
        Log.i(TAG, "[test10_KeyStoreDelete()] END");
    }
    void test11_ReadOnly() throws Exception {
        Log.i(TAG, "[test11_ReadOnly()] START");
        setUp();

        {
            Transaction t = new Transaction(db);
            t.set(new Slice("key".getBytes()), new Slice("value".getBytes()));
            t.delete();
        }

        // Reopen db as read-only:
        db.delete();
        db = null;
        Config config = Database.defaultConfig();
        config.setFlags(OpenFlags.FDB_OPEN_FLAG_RDONLY);
        File dbFile = new File(activity.getFilesDir(), dbfilename);
        db = new Database(dbFile.getPath(), config);

        Document doc = db.get(new Slice("key".getBytes()));
        if(!doc.exists())
            Log.e(TAG, "doc should exist: " + doc.exists());

        // Attempt to change a doc:
        int status = 0;
        Transaction t = new Transaction(db);
        try{
            t.set(new Slice("key".getBytes()), new Slice("somethingelse".getBytes()));
        }catch(Exception ex){
            status = Integer.valueOf(ex.getMessage());
        }
        finally {
            t.delete();
        }
        if(status != -10)
            Log.e(TAG, "Exception should be -10 (FDB_RESULT_RONLY_VIOLATION): " + status);

        // Now try to open a nonexistent db, without the CREATE flag:
        status = 0;
        try{
            Database db2 = new Database(dbFile.getPath()+"_non_existent", config);
        }
        catch(Exception ex){
            status = Integer.valueOf(ex.getMessage());
        }
        if(status != -3)
            Log.e(TAG, "Exception should be -3 (FDB_RESULT_NO_SUCH_FILE): " + status);
        tearDown();
        Log.i(TAG, "[test11_ReadOnly()] END");
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
