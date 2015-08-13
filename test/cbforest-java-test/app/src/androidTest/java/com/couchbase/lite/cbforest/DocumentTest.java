package com.couchbase.lite.cbforest;

import android.util.Log;

/**
 * Created by hideki on 8/12/15.
 */
public class DocumentTest extends BaseCBForestTestCase {
    public static final String TAG = DocumentTest.class.getSimpleName();

    public void test00_Document() {
        Document doc = new Document(new Slice("key".getBytes()));
        assertEquals("key", new String(doc.getKey().getBuf()));
        doc.setKey(new Slice("ABCDE".getBytes()));
        doc.setMeta(new Slice("abcde".getBytes()));
        doc.setBody(new Slice("12345".getBytes()));
        assertEquals("ABCDE", new String(doc.getKey().getBuf()));
        assertEquals("abcde", new String(doc.getMeta().getBuf()));
        assertEquals("12345", new String(doc.getBody().getBuf()));
        doc.delete();
        doc = new Document();
        Log.i(TAG, "doc.getKey().getBuf()=" + doc.getKey().getBuf());
        Log.i(TAG, "doc.getMeta().getBuf()=" + doc.getMeta().getBuf());
        Log.i(TAG, "doc.getBody().getBuf()=" + doc.getBody().getBuf());
    }
}
