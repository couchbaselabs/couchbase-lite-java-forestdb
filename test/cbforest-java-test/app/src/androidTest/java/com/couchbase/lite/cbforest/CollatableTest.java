package com.couchbase.lite.cbforest;

import android.util.Log;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Created by hideki on 8/13/15.
 */
public class CollatableTest extends BaseCBForestTestCase {
    public static final String TAG = CollatableTest.class.getSimpleName();

    static int sgn(int n) {
        return n < 0 ? -1 : (n > 0 ? 1 : 0);
    }

    static int compareCollated(boolean obj1, boolean obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1);
        c2.add(obj2);
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    static int compareCollated(double obj1, double obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1);
        c2.add(obj2);
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    static int compareCollated(double obj1, boolean obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1);
        c2.add(obj2);
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    static int compareCollated(boolean obj1, double obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1);
        c2.add(obj2);
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    static int compareCollated(BigDecimal obj1, BigDecimal obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1.doubleValue());
        c2.add(obj2.doubleValue());
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    static int compareCollated(String obj1, String obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1);
        c2.add(obj2);
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    static int compareCollated(String obj1, double obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1);
        c2.add(obj2);
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    static int compareCollated(String obj1, boolean obj2) {
        Collatable c1 = new Collatable();
        Collatable c2 = new Collatable();
        c1.add(obj1);
        c2.add(obj2);
        Slice s1 = c1.toSlice();
        Slice s2 = c2.toSlice();
        int ret = sgn(s1.compare(s2));
        c1.delete();
        c2.delete();
        s1.delete();
        s2.delete();
        return ret;
    }

    boolean compareNumber(BigDecimal n1, BigDecimal n2) {
        return compareCollated(n1, n2) == n1.compareTo(n2);
    }

    boolean compareNumber(double n1, double n2) {
        return compareCollated(n1, n2) == Double.valueOf(n1).compareTo(Double.valueOf(n2));
    }

    boolean checkRoundTrip(double v) throws Exception {
        Collatable c = new Collatable();
        c.add(v);
        Slice s = c.toSlice();
        CollatableReader reader = new CollatableReader(s);
        double d = reader.readDouble();
        boolean ret = d == v;
        reader.delete();
        s.delete();
        c.delete();
        return ret;
    }

    public void testScalars() {
        assertEquals(1, compareCollated(true, false));
        assertEquals(-1, compareCollated(false, true));
        assertEquals(0, compareCollated(false, false));
        assertEquals(0, compareCollated(true, true));
        assertEquals(-1, compareCollated(true, 17));
        assertEquals(0, compareCollated(1, 1));
        assertEquals(1, compareCollated(123, 1));
        assertEquals(1, compareCollated(0x100, 0xFF));
        assertEquals(1, compareCollated(0x1234, 0x12));
        assertEquals(1, compareCollated(0x1234, 0x13));
        assertEquals(1, compareCollated(Long.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(-1, compareCollated(Integer.valueOf(-1), Integer.valueOf(0)));
        assertEquals(-1, compareCollated(Integer.valueOf(-1), Integer.valueOf(1)));
        assertEquals(-1, compareCollated(Integer.valueOf(-123), Integer.valueOf(-7)));
    }

    public void testRandomNumbers() {
        Random rand = new Random();
        for (int i = 0; i < 10000; i++) {
            long n1 = rand.nextLong();
            long n2 = rand.nextLong();
            BigDecimal bd1 = BigDecimal.valueOf(n1);
            BigDecimal bd2 = BigDecimal.valueOf(n2);
            assertTrue(compareNumber(bd1, bd2));
        }
    }

    public void testFloats() throws Exception {
        double numbers[] = {0, 1, 2, 10, 32, 63, 64, 256, Math.PI, 100, 6.02e23, 6.02e-23, 0.01,
                Double.MAX_VALUE, Double.MIN_VALUE,
                Math.PI + 0.1, Math.PI - 0.1,
                -1, -64, -Math.PI, -6.02e23};

        for (int i = 0; i < numbers.length; i++) {
            assertTrue(checkRoundTrip(numbers[i]));
            for (int j = 0; j < numbers.length; j++) {
                assertTrue(compareNumber(BigDecimal.valueOf(numbers[i]), BigDecimal.valueOf(numbers[j])));
            }
        }
    }

    public void testRandomFloats() throws Exception {

        Random rand = new Random();
        for (int i = 0; i < 100000; i++) {
            double n1 = rand.nextDouble();
            double n2 = rand.nextDouble();
            assertTrue(checkRoundTrip(n1));
            assertTrue(checkRoundTrip(n2));
            // NOTE: BigDecimal comparision is tooooooooo slow.....
            //assertTrue(compareNumber(BigDecimal.valueOf(n1), BigDecimal.valueOf(n2)));
            assertTrue(compareNumber(n1, n2));
        }
    }

    public void testRoundTripInts() throws Exception {
        long n = 1;
        // 63 (not 64): long is signed long
        for (int bits = 0; bits < 63; ++bits, n <<= 1) {
            Collatable c = new Collatable();
            c.add(n - 1);
            Slice encoded = c.toSlice();
            CollatableReader reader = new CollatableReader(encoded);
            long result = reader.readInt();
            Log.i(TAG, "2^" + bits + " - 1: " + (n - 1) + " --> " + result);
            if (bits < 54)
                assertEquals(n - 1, result);
        }
    }

    public void testStrings() {
        assertEquals(1, compareCollated("", 7));
        assertEquals(0, compareCollated("", ""));
        assertEquals(1, compareCollated("", true));
        assertEquals(-1, compareCollated("", " "));
        assertEquals(-1, compareCollated("~", "a"));
        assertEquals(1, compareCollated("A", "a"));
        assertEquals(-1, compareCollated("\n", " "));
        assertEquals(1, compareCollated("Hello world", ""));
        assertEquals(1, compareCollated("Hello world", "Aaron"));
        assertEquals(-1, compareCollated("Hello world", "Hello world!"));
        assertEquals(-1, compareCollated("hello World", "hellO wOrLd"));
        assertEquals(-1, compareCollated("Hello world", "jello world"));
        assertEquals(-1, compareCollated("hello world", "Jello world"));
        assertEquals(-1, compareCollated("Hello world", "Hello wörld!"));
    }

    public void testIndexKey() throws Exception {
        String key = "OR";
        Collatable collKey = new Collatable();
        collKey.add(key);

        String docID = "foo";
        Collatable collatableDocID = new Collatable();
        collatableDocID.add(docID);

        Collatable indexKey = new Collatable();
        indexKey.beginArray();
        indexKey.add(collKey);
        indexKey.add(collatableDocID);
        indexKey.add(1234);
        indexKey.endArray();

        Slice encoded = indexKey.toSlice();

        CollatableReader reader = new CollatableReader(encoded);
        reader.beginArray();
        Slice readKey = reader.readString();
        Log.i(TAG, new String(readKey.getBuf()));
        assertTrue(readKey.compare(new Slice(key.getBytes())) == 0);
        assertTrue(new String(readKey.getBuf()).equals(key));


        Slice readDocID = reader.readString();
        Log.i(TAG, new String(readDocID.getBuf()));
        assertTrue(readDocID.compare(new Slice(docID.getBytes())) == 0);
        assertTrue(new String(readDocID.getBuf()).equals(docID));
        assertEquals(1234, reader.readInt());
    }
}
