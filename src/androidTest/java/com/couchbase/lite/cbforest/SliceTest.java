package com.couchbase.lite.cbforest;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by hideki on 8/12/15.
 */
public class SliceTest  extends BaseCBForestTestCase  {
    public static final String TAG = SliceTest.class.getSimpleName();

    public void test01_Slice(){
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

    public void test02_GetData(){
        String source = "こんにちは ForestDB!!!";

        Slice slice = new Slice(source.getBytes());

        byte[] data = cbforest.cdata(slice.getData(), slice.getSize());
        String obtained = new String(data);

        assertEquals(source, obtained);
        assertEquals(source.getBytes().length, slice.getSize());
        assertTrue(Arrays.equals(source.getBytes(), data));

        slice.delete();
    }

}
