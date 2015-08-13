package com.couchbase.lite.cbforest;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hideki on 8/13/15.
 */
public class MapReduceTest extends BaseCBForestTestCase {
    public static final String TAG = MapReduceTest.class.getSimpleName();

    // Object -> JSON string
    static String obj2json(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // JSON -> Object
    static Map<String, Object> json2obj(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class TestJSONMappable extends Mappable {
        Map<String, Object> body = null;

        public TestJSONMappable(Document doc) {
            super(doc);

            if (doc.deleted())
                body = null;
            else {
                String json = new String(doc.getBody().getBuf());
                Log.i(TAG, json);
                body = json2obj(json);
                Log.i(TAG, "body => " + body.toString());
            }
        }
    }

    public static class TestMapFn extends MapFn {
        public static int numMapCalls = 0;

        public void call(Mappable mappable, EmitFn emit) {
            numMapCalls++;
            TestJSONMappable testJSONMappable = (TestJSONMappable) mappable;
            Map<String, Object> body = testJSONMappable.body;
            if (body != null) {
                Object obj = body.get("cities");
                if (obj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> cities = (List<String>) obj;
                    for (String city : cities) {
                        Collatable key = new Collatable();
                        key.add(city);
                        Collatable value = new Collatable();
                        String name = (String) body.get("name");
                        value.add(name);
                        emit.call(key, value);
                    }
                }
            }
        }
    }

    public static class TestIndexer extends MapReduceIndexer {
        public static boolean updateIndex(Database database, MapReduceIndex index) {
            Transaction trans = new Transaction(database);
            TestIndexer indexer = new TestIndexer();
            indexer.addIndex(index, trans);
            boolean r = indexer.run();
            indexer.delete();
            trans.delete();
            return r;
        }

        public void addDocument(Document doc) {
            TestJSONMappable mappable = new TestJSONMappable(doc);
            addMappable(mappable);
        }
    }

    private KeyStore source = null;
    private MapReduceIndex index = null;

    protected void setUp() throws Exception {
        super.setUp();

        source = db;

        File indexFile = new File(mContext.getFilesDir(), DB_FILENAME + "index");
        if (indexFile.exists()) {
            if (!indexFile.delete()) {
                fail();
            }
        }

        index = new MapReduceIndex(db, "index", source);
        assertNotNull(index);
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        if (index != null) {
            index.delete();
            index = null;
        }
    }

    void queryExpectingKeys(List<String> expectedKeys) throws Exception {
        TestMapFn.numMapCalls = 0;
        assertTrue(TestIndexer.updateIndex(db, index));
        int nRows = 0;
        IndexEnumerator e = new IndexEnumerator(index, new Collatable(), new Slice(), new Collatable(), new Slice(), new DocEnumerator.Options());
        for (; e.next(); nRows++) {
            String key = new String(e.key().readString().getBuf());
            String docID = new String(e.docID().getBuf());
            Log.i(TAG, String.format("key => %s, docID => %s", key, docID));
            assertEquals(expectedKeys.get(nRows), key);
        }
        assertEquals(expectedKeys.size(), nRows);
        assertEquals(index.rowCount().intValue(), nRows);
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

    public void testMapReduce() throws Exception {

        TestMapFn mapFn = new TestMapFn();
        createDocsAndIndex(mapFn);

        Log.i(TAG, "--- First query");
        TestMapFn.numMapCalls = 0;
        this.queryExpectingKeys(Arrays.asList("Cambria", "Eugene", "Port Townsend", "Portland",
                "San Francisco", "San Jose", "Seattle", "Skookumchuk"));
        assertEquals(3, TestMapFn.numMapCalls);

        Log.i(TAG, "--- Updating OR");
        {
            Transaction trans = new Transaction(db);

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("name", "Oregon");
            body.put("cities", Arrays.asList("Portland", "Walla Walla", "Salem"));

            String json = obj2json(body);

            trans.set(new Slice("OR".getBytes()), new Slice(), new Slice(json.getBytes()));

            trans.delete();
        }
        this.queryExpectingKeys(Arrays.asList("Cambria", "Port Townsend", "Portland", "Salem", "San Francisco", "San Jose", "Seattle", "Skookumchuk", "Walla Walla"));
        assertEquals(1, TestMapFn.numMapCalls);

        Log.i(TAG, "--- Deleting CA");
        {
            Transaction trans = new Transaction(db);
            trans.del(new Slice("CA".getBytes()));
            trans.delete();
        }
        this.queryExpectingKeys(Arrays.asList("Port Townsend", "Portland", "Salem", "Seattle", "Skookumchuk", "Walla Walla"));
        assertEquals(0, TestMapFn.numMapCalls);

        Log.i(TAG, "--- Updating version");
        TestMapFn mapFn2 = new TestMapFn();
        {
            Transaction trans = new Transaction(db);
            index.setup(trans, 0, mapFn2, "2");
            trans.delete();
        }
        this.queryExpectingKeys(Arrays.asList("Port Townsend", "Portland", "Salem", "Seattle", "Skookumchuk", "Walla Walla"));
        assertEquals(2, TestMapFn.numMapCalls);

        mapFn2.delete();
        mapFn.delete();
    }

    public void testReopen() throws Exception {
        TestMapFn mapFn = new TestMapFn();
        createDocsAndIndex(mapFn);

        assertTrue(TestIndexer.updateIndex(db, index));

        BigInteger lastIndexed = index.lastSequenceIndexed();
        BigInteger lastChangedAt = index.lastSequenceIndexed();

        assertTrue(lastChangedAt.intValue() > 0);
        assertTrue(lastIndexed.intValue() >= lastChangedAt.intValue());

        index.delete();
        index = null;

        index = new MapReduceIndex(db, "index", source);
        assertNotNull(index);

        TestMapFn mapFn2 = new TestMapFn();
        {
            Transaction trans = new Transaction(db);
            index.setup(trans, 0, mapFn2, "1");
            trans.delete();
        }
        assertTrue(index.lastSequenceIndexed().intValue() == lastIndexed.intValue());
        assertTrue(index.lastSequenceChangedAt().intValue() == lastChangedAt.intValue());

        mapFn.delete();
        mapFn2.delete();
    }
}
