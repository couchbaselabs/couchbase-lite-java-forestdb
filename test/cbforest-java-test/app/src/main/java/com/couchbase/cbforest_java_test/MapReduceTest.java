package com.couchbase.cbforest_java_test;

import android.app.Activity;
import android.util.Log;

import com.couchbase.lite.cbforest.Collatable;
import com.couchbase.lite.cbforest.Database;
import com.couchbase.lite.cbforest.DocEnumerator;
import com.couchbase.lite.cbforest.Document;
import com.couchbase.lite.cbforest.EmitFn;
import com.couchbase.lite.cbforest.IndexEnumerator;
import com.couchbase.lite.cbforest.KeyStore;
import com.couchbase.lite.cbforest.MapFn;
import com.couchbase.lite.cbforest.MapReduceIndex;
import com.couchbase.lite.cbforest.MapReduceIndexer;
import com.couchbase.lite.cbforest.Mappable;
import com.couchbase.lite.cbforest.Slice;
import com.couchbase.lite.cbforest.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapReduceTest {

	public static final String TAG = "MapReduceTest";

	// Object -> JSON string
    static String obj2json(Object obj){
    	ObjectMapper mapper = new ObjectMapper();
    	try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    // JSON -> Object
    static Map<String, Object> json2obj(String json){
    	ObjectMapper mapper = new ObjectMapper();
    	try {
			return mapper.readValue(json, new TypeReference<Map<String,Object>>(){});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }
    
	public static class TestJSONMappable extends Mappable {
		Map<String, Object> body = null;
		public TestJSONMappable(Document doc) {
			super(doc);

			if(doc.deleted())
				body = null;
			else{
				String json = new String(doc.getBody().getBuf());
				Log.i(TAG, json);
				body = json2obj(json);	
				Log.i(TAG, "body => "+body.toString());
			}
		}
	}
	public static class TestMapFn extends MapFn {
		public static int numMapCalls = 0;
		
		public void call(Mappable mappable, EmitFn emit) {
			Log.i(TAG, "[TestMapFn.call()] START");
			numMapCalls++;
			if(mappable instanceof TestJSONMappable)
				Log.i(TAG, "mappable instanceof TestJSONMappable => true");
			else
				Log.i(TAG, "mappable instanceof TestJSONMappable => false");
			TestJSONMappable testJSONMappable = (TestJSONMappable)mappable;
			Map<String, Object> body = testJSONMappable.body;
			if(body != null){
				Object obj = body.get("cities");
				Log.i(TAG, "obj => " + obj.toString());
				if(obj instanceof List){
					@SuppressWarnings("unchecked")
					List<String> cities = (List<String>)obj;
					for(String city : cities){
						Log.i(TAG, "city => " + city);
						Collatable key = new Collatable();
						key.add(city);
						Collatable value = new Collatable();
						String name = (String)body.get("name");
						Log.i(TAG, "name => " + name);
						value.add(name);
						Log.i(TAG, "emit.call()");
						emit.call(key, value);
						Log.i(TAG, "emit.call() end");
					}
				}
			}
            Log.i(TAG, "[TestMapFn.call()] END");
		}	
	}

	public static class TestIndexer extends MapReduceIndexer {
		public static boolean updateIndex(Database database, MapReduceIndex index){
            Log.i(TAG, "[TestIndexer.updateIndex()] START");
			Transaction trans = new Transaction(database);
			TestIndexer indexer = new TestIndexer();
            indexer.addIndex(index, trans);
            boolean r = indexer.run();
            indexer.delete();
            trans.delete();
            Log.i(TAG, "[TestIndexer.updateIndex()] END");
			return r;
		}

		public void addDocument(Document doc) {
            Log.i(TAG, "[TestIndexer.addDocument()] START");
			TestJSONMappable mappable = new TestJSONMappable(doc);
			addMappable(mappable);
            Log.i(TAG, "[TestIndexer.addDocument()] END");
		}
	}

	public static final String db_filename = "forest_temp.fdb";
    public static final String index_filename = db_filename+ "index";

    private Activity activity = null;
    
    Database db = null;
    KeyStore source = null;
    MapReduceIndex index = null;
    
    public MapReduceTest(Activity activity){
        this.activity = activity;
    }
    
    public void test() throws Exception {
    	//testMapReduce();
		//testReopen();
    	testAll();
    }
    void testAll() throws Exception {
    	testMapReduce();
		testReopen();
    }
    
    void setUp() throws Exception {
		Log.i(TAG, "[setUp()] START");
        File dbFile = new File(activity.getFilesDir(), db_filename);
        Log.i(TAG, "dbFile="+dbFile);
        if(dbFile.exists()){
            if(!dbFile.delete()){
                Log.e(TAG, "ERROR failed to delete: dbFile="+dbFile);
            }
        }
        db = new Database(dbFile.getPath(), Database.defaultConfig());

        source = db;
        
        File indexFile = new File(activity.getFilesDir(), index_filename);
        Log.i(TAG, "indexFile=" + indexFile);
		if(indexFile.exists()){
            if(!indexFile.delete()){
                Log.e(TAG, "ERROR failed to delete: indexFile="+indexFile);
            }
        }
        index = new MapReduceIndex(db, "index", source);
		Log.i(TAG, "[setUp()] END");
    }

    void tearDown(){
        Log.i(TAG, "[tearDown()] START");
		index.delete();
		db.delete();
        Log.i(TAG, "[tearDown()] END");
    }
    
    void queryExpectingKeys(List<String> expectedKeys) throws Exception {
    	Log.i(TAG, "[queryExpectingKeys()] START");

		TestMapFn.numMapCalls = 0;
    	boolean res = TestIndexer.updateIndex(db, index);
    	if(!res)
    		Log.e(TAG, "ERROR: TestIndexer Failed");

		int nRows = 0;
		Log.i(TAG, "create IndexEnumerator START");
		IndexEnumerator e = new IndexEnumerator(index, new Collatable(), new Slice(), new Collatable(), new Slice(), new DocEnumerator.Options());
		Log.i(TAG, "create IndexEnumerator DONE");
		for (; e.next(); nRows++) {
			String key = new String(e.key().readString().getBuf());
			String docID = new String(e.docID().getBuf());
			Log.i(TAG, String.format("key => %s, docID => %s", key, docID));
			if (!key.equals(expectedKeys.get(nRows)))
				Log.e(TAG, String.format("%s != %s", key, expectedKeys.get(nRows)));
		}

		Log.i(TAG, "nRows=>" + nRows + " index.rowCount()=>" + index.rowCount().intValue());
        if(nRows != expectedKeys.size())
        	Log.e(TAG, "nRows != expectedKeys.size()");
        if(nRows != index.rowCount().intValue())
        	Log.e(TAG, "nRows != index.rowCount().intValue()");

        Log.i(TAG, "[queryExpectingKeys()] END");
    }

	void createDocsAndIndex(MapFn mapFn) throws Exception {

		{
			// Populate the database:
			Map<String, Object> doc1 = new HashMap<String, Object>();
			doc1.put("name", "California");
			doc1.put("cities", Arrays.asList("San Jose", "San Francisco", "Cambria"));

			Map<String, Object> doc2 = new HashMap<String, Object>();
			doc2.put("name", "Washington");
			doc2.put("cities", Arrays.asList("Seattle", "Port Townsend", "Skookumchuk"));

			Map<String, Object> doc3 = new HashMap<String, Object>();
			doc3.put("name", "Oregon");
			doc3.put("cities", Arrays.asList("Portland", "Eugene"));

			Map<String, Map<String, Object>> data = new HashMap<String, Map<String, Object>>();
			data.put("CA", doc1);
			data.put("WA", doc2);
			data.put("OR", doc3);

			Transaction trans = new Transaction(db);

			for (String docID : data.keySet()) {
				Map<String, Object> doc = data.get(docID);
				String json = obj2json(doc);
				Log.i(TAG, json);
				trans.set(new Slice(docID.getBytes()), new Slice(), new Slice(json.getBytes()));
			}
			trans.delete();
		}


		{
			Transaction trans1 = new Transaction(db);
			index.setup(trans1, 0, mapFn, "1");
			trans1.delete();
		}

	}

	void testMapReduce() throws Exception {
		Log.i(TAG, "[testMapReduce()] START");

    	setUp();

		TestMapFn mapFn = new TestMapFn();
		createDocsAndIndex(mapFn);

		Log.i(TAG, "--- First query");
    	TestMapFn.numMapCalls = 0;
    	this.queryExpectingKeys(Arrays.asList("Cambria", "Eugene", "Port Townsend", "Portland",
				"San Francisco", "San Jose", "Seattle", "Skookumchuk"));
    	if(TestMapFn.numMapCalls != 3)
    		Log.e(TAG, "TestMapFn.numMapCalls != 3");

    	Log.i(TAG, "--- Updating OR");
    	{
    		Transaction trans = new Transaction(db);
    		
    		Map<String, Object> body = new HashMap<String, Object>();
    		body.put("name", "Oregon");
    		body.put("cities", Arrays.asList("Portland", "Walla Walla", "Salem"));
    		
    		String json = obj2json(body);
        	Log.i(TAG, json);

			trans.set(new Slice("OR".getBytes()), new Slice(), new Slice(json.getBytes()));
    		
    		trans.delete();
    	}
    	this.queryExpectingKeys(Arrays.asList("Cambria", "Port Townsend", "Portland", "Salem", "San Francisco", "San Jose", "Seattle", "Skookumchuk", "Walla Walla"));
    	if(TestMapFn.numMapCalls != 1)
    		Log.e(TAG, "TestMapFn.numMapCalls != 1");

		Log.i(TAG, "--- Deleting CA");
    	{
    		Transaction trans = new Transaction(db);
    		trans.del(new Slice("CA".getBytes()));
    		trans.delete();
    	}
    	this.queryExpectingKeys(Arrays.asList("Port Townsend", "Portland", "Salem", "Seattle", "Skookumchuk", "Walla Walla"));
    	if(TestMapFn.numMapCalls != 0)
    		Log.e(TAG, "TestMapFn.numMapCalls != 0");


		Log.i(TAG, "--- Updating version");
		TestMapFn mapFn2 = new TestMapFn();
		{
			Transaction trans = new Transaction(db);
			index.setup(trans, 0, mapFn2, "2");
			trans.delete();
		}
		this.queryExpectingKeys(Arrays.asList("Port Townsend", "Portland", "Salem", "Seattle", "Skookumchuk", "Walla Walla"));
		if(TestMapFn.numMapCalls != 2)
			Log.e(TAG, "TestMapFn.numMapCalls != 2");

		mapFn2.delete();
		mapFn.delete();

    	tearDown();

        Log.i(TAG, "[testMapReduce()] END");
    }

	void testReopen() throws Exception{
		Log.i(TAG, "[testReopen()] START");

		setUp();

		TestMapFn mapFn = new TestMapFn();
		createDocsAndIndex(mapFn);

		if(!TestIndexer.updateIndex(db, index))
			Log.e(TAG, "TestIndexer.updateIndex(db, index) should return true!");

		BigInteger lastIndexed = index.lastSequenceIndexed();
		BigInteger lastChangedAt = index.lastSequenceIndexed();
		if (lastChangedAt.intValue() <= 0)
			Log.e(TAG, "Should be lastChangedAt.intValue() > 0");
		if (lastIndexed.intValue() < lastChangedAt.intValue())
			Log.e(TAG, "Should Be lastIndexed.intValue() >= lastChangedAt.intValue()");

		index.delete();
		index = null;

		index = new MapReduceIndex(db, "index", source);
		if(index == null)
			Log.e(TAG, "Couldn't reopen index");

		TestMapFn mapFn2 = new TestMapFn();
		{
			Transaction trans = new Transaction(db);
			index.setup(trans, 0, mapFn2, "1");
			trans.delete();
		}
		if (index.lastSequenceIndexed().intValue() != lastIndexed.intValue())
			Log.e(TAG, "Should be index.lastSequenceIndexed().intValue() == lastIndexed.intValue()");
		if (index.lastSequenceChangedAt().intValue() != lastChangedAt.intValue())
			Log.e(TAG, "Should Be index.lastSequenceChangedAt().intValue() == lastChangedAt.intValue()");

		mapFn.delete();
		mapFn2.delete();

		tearDown();

		Log.i(TAG, "[testReopen()] END");

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
