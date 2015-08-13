package com.couchbase.lite.cbforest;

import android.util.Log;

/**
 * Created by hideki on 8/13/15.
 */
public class RevisionTest extends BaseCBForestTestCase  {
    public static final String TAG = RevisionTest.class.getSimpleName();

    public void testRevIDBuffer() throws Exception {
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
    }
}
