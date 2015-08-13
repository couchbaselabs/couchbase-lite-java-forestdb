package com.couchbase.lite.cbforest;

import android.util.Log;

/**
 * Created by hideki on 8/12/15.
 */
public class SliceTest  extends BaseCBForestTestCase  {
    public static final String TAG = SliceTest.class.getSimpleName();

    public void test00_Slice(){
        String str = "こんにちは ForestDB!!!";

        Slice s= new Slice(str.getBytes());
        Log.i(TAG, "s.getBuf()=" + new String(s.getBuf()));
        String sliceStr = new String(s.getBuf());
        assertEquals(str, new String(s.getBuf()));
        assertEquals(str.getBytes().length, s.getSize());
        s.delete();

        // default constructor
        s  = new Slice();
        assertNotNull(s);
        assertEquals(0, s.getSize());
        s.delete();
    }
}
