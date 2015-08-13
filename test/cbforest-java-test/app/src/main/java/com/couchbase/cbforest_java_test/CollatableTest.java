package com.couchbase.cbforest_java_test;

import android.app.Activity;
import android.util.Log;

import com.couchbase.lite.cbforest.Collatable;
import com.couchbase.lite.cbforest.CollatableReader;
import com.couchbase.lite.cbforest.Slice;

import java.math.BigDecimal;
import java.util.Random;


public class CollatableTest {
	public static String TAG = "CollatableTest";

    public CollatableTest(Activity activity){
    }

    public void test() throws Exception {

        /*
        // NOTE: Following two tests create 10K Collatable instances.
        //       It seems creating 10K native instances causes memory leak even though calling delete().
        //       On Genymotion Emulator, memory leak is not observed. So it could be platform issue?
    	testRandomNumbers();
		testRandomFloats();

        testScalars();
        testFloats();
    	testRoundTripInts();
    	testStrings();
    	testIndexKey();
    	*/

    	testAll();
    }
    void testAll() throws Exception {
    	testScalars();
    	testRandomNumbers();
    	testFloats();
    	testRandomFloats();
    	testRoundTripInts();
    	testStrings();
    	testIndexKey();    	
    }
    
    static int sgn(int n) {return n<0 ? -1 : (n>0 ? 1 : 0);}

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
        int ret =  sgn(s1.compare(s2));
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

    boolean compareNumber(BigDecimal n1, BigDecimal n2){
    	return compareCollated(n1, n2) == n1.compareTo(n2);
    }

    boolean compareNumber(double n1, double n2){
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
    
    void testScalars(){
    	Log.i(TAG, "[testScalars()] START");
    	
    	if(compareCollated(true, false) != 1)
    		Log.e(TAG, "compareCollated(true, false) != 1");
    	if(compareCollated(false, true) != -1)
    		Log.e(TAG, "compareCollated(false, true) != -1");
    	if(compareCollated(false, false) != 0)
    		Log.e(TAG, "compareCollated(false, false) != 0");
    	if(compareCollated(true, true) != 0)
    		Log.e(TAG, "compareCollated(true, true) != 0");
    	if(compareCollated(true, 17) != -1)
    		Log.e(TAG, "compareCollated(true, 17) != -1");
    	if(compareCollated(1, 1) != 0)
    		Log.e(TAG, "compareCollated(1, 1) != 0");
    	if(compareCollated(123, 1) != 1)
    		Log.e(TAG, "compareCollated(123, 1) != 1");
    	if(compareCollated(0x100, 0xFF) != 1)
    		Log.e(TAG, "compareCollated(0x100, 0xFF) != 1");
    	if(compareCollated(0x1234, 0x12) != 1)
    		Log.e(TAG, "compareCollated(0x1234, 0x12) != 1");
    	if(compareCollated(0x1234, 0x13) != 1)
    		Log.e(TAG, "compareCollated(0x1234, 0x13) != 1");
    	if(compareCollated(Long.MAX_VALUE, Integer.MAX_VALUE) != 1)
    		Log.e(TAG, "compareCollated(Long.MAX_VALUE, Integer.MAX_VALUE) != 1");
    	
    	if(compareCollated(Integer.valueOf(-1),Integer.valueOf(0)) != -1)
    		Log.e(TAG, "compareCollated(Integer.valueOf(-1),Integer.valueOf(0)) != -1");
    	if(compareCollated(Integer.valueOf(-1),Integer.valueOf(1)) != -1)
    		Log.e(TAG, "compareCollated(Integer.valueOf(-1),Integer.valueOf(1)) != -1");
    	if(compareCollated(Integer.valueOf(-123),Integer.valueOf(-7)) != -1)
    		Log.e(TAG, "compareCollated(Integer.valueOf(-123),Integer.valueOf(-7)) != -1");

		Log.i(TAG, "[testScalars()] END");
    }

    void testRandomNumbers() {
		Log.i(TAG, "[testRandomNumbers()] START");
        Random rand = new Random();
        for (int i = 0; i < 10000; i++) {
            long n1 = rand.nextLong();
            long n2 = rand.nextLong();
            BigDecimal bd1 = BigDecimal.valueOf(n1);
            BigDecimal bd2 = BigDecimal.valueOf(n2);
            if (!compareNumber(bd1, bd2))
                Log.e(TAG, "compareNumber(" + n1 + ", " + n2 + ")");
        }
		Log.i(TAG, "[testRandomNumbers()] END");
    }
    
    void testFloats() throws Exception {
		Log.i(TAG, "[testFloats()] START");
    	
    	double numbers[] = {0, 1, 2, 10, 32, 63, 64, 256, Math.PI, 100, 6.02e23, 6.02e-23, 0.01,
    	        Double.MAX_VALUE, Double.MIN_VALUE,
    	        Math.PI + 0.1, Math.PI - 0.1,
    	        -1, -64, -Math.PI, -6.02e23};
    	
    	final int nFloats = numbers.length;
    	
    	for (int i=0; i<nFloats; i++) {
    		if(!checkRoundTrip(numbers[i]))
    			Log.e(TAG, "checkRoundTrip("+numbers[i] +")");
    		for(int j = 0; j < nFloats; j++){
    			if(!compareNumber(BigDecimal.valueOf(numbers[i]), BigDecimal.valueOf(numbers[j])))
    				Log.e(TAG, "compareNumber("+numbers[i] + ", " + numbers[j] +")");
    		}
    	}

		Log.i(TAG, "[testFloats()] END");
    }
    
    void testRandomFloats() throws Exception {
		Log.i(TAG, "[testRandomFloats()] START");
    	Random rand = new Random();
    	for (int i=0; i< 100000; i++) {
    		double n1 = rand.nextDouble();
    		double n2 = rand.nextDouble();
			if(!checkRoundTrip(n1))
    			Log.e(TAG, "checkRoundTrip("+n1 +")");
			if(!checkRoundTrip(n2))
    			Log.e(TAG, "checkRoundTrip("+n2 +")");

			// NOTE: BigDecimal comparision is tooooooooo slow.....
    		//if(!compareNumber(BigDecimal.valueOf(n1), BigDecimal.valueOf(n2)))
			//	Log.e(TAG, "compareNumber("+n1 + ", " + n2 +")");

    		if(!compareNumber(n1, n2))
				Log.e(TAG, "compareNumber("+n1 + ", " + n2 +")");
    	}

		Log.i(TAG, "[testRandomFloats()] END");
    }
    void testRoundTripInts() throws Exception {
		Log.i(TAG, "[testRoundTripInts()] START");
    	long n = 1;
    	// 63 (not 64): long is signed long
    	for(int bits = 0; bits < 63; ++bits, n <<= 1){
    		Collatable c = new Collatable();
    		c.add(n - 1);
    		Slice encoded = c.toSlice();
    		CollatableReader reader = new CollatableReader(encoded);
    		long result = reader.readInt();
    		Log.i(TAG, "2^"+ bits +" - 1: " + (n - 1) + " --> " + result);
    		if(bits < 54)
    			if(result != n - 1)
    				Log.e(TAG, "result != n - 1");
    	}

		Log.i(TAG, "[testRoundTripInts()] END");
    }
    void testStrings(){
		Log.i(TAG, "[testStrings()] START");
    	
    	if(compareCollated("", 7) != 1)
    		Log.e(TAG, "compareCollated(\"\",7) != 1");
    	if(compareCollated("", "") != 0)
    		Log.e(TAG, "compareCollated(\"\", \"\") != 0");
    	if(compareCollated("", true) != 1)
    		Log.e(TAG, "compareCollated(\"\", true) != 1");
    	if(compareCollated("", " ") != -1)
    		Log.e(TAG, "compareCollated(\"\", \" \") != -1");
    	if(compareCollated("~", "a") != -1)
    		Log.e(TAG, "compareCollated(\"~\", \"a\") != -1");
    	if(compareCollated("A", "a") != 1)
    		Log.e(TAG, "compareCollated(\"A\", \"a\") != 1");
    	if(compareCollated("\n", " ") != -1)
    		Log.e(TAG, "compareCollated(\"\n\", \" \") != -1");
    	if(compareCollated("Hello world", "") != 1)
    		Log.e(TAG, "compareCollated(\"Hello world\", \"\") != 0");
    	if(compareCollated("Hello world", "Aaron") != 1)
    		Log.e(TAG, "compareCollated(\"Hello world\", \"Aaron\") != 1");
    	if(compareCollated("Hello world", "Hello world!") != -1)
    		Log.e(TAG, "compareCollated(\"Hello world\", \"Hello world!\") != -1");
    	if(compareCollated("hello World", "hellO wOrLd") != -1)
    		Log.e(TAG, "compareCollated(\"hello World\", \"hellO wOrLd\") != -1");
    	if(compareCollated("Hello world", "jello world") != -1)
    		Log.e(TAG, "compareCollated(\"Hello world\", \"jello world\") != 1");
    	if(compareCollated("hello world", "Jello world") != -1)
    		Log.e(TAG, "compareCollated(\"hello world\", \"Jello world\") != -1");
    	if(compareCollated("Hello world", "Hello wörld!") != -1)
    		Log.e(TAG, "compareCollated(\"Hello world\", \"Hello wörld!\") != -1");
		Log.i(TAG, "[testStrings()] END");
    }

    void testIndexKey() throws Exception {
		Log.i(TAG, "[testIndexKey()] START");
    	
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
    	if(readKey.compare(new Slice(key.getBytes())) != 0)
    		Log.e(TAG, "readKey.compare(new Slice(key)) != 0");
    	if(!new String(readKey.getBuf()).equals(key))
    		Log.e(TAG, "!readKey.getBuf().equals(key)");

    	Slice readDocID = reader.readString();
    	Log.i(TAG, new String(readDocID.getBuf()));
    	if(readDocID.compare(new Slice(docID.getBytes())) != 0)
    		Log.e(TAG, "readDocID.compare(new Slice(docID)) != 0");
    	if(!new String(readDocID.getBuf()).equals(docID))
    		Log.e(TAG, "!readDocID.getBuf().equals(docID)");
    	long readSequence = reader.readInt();
    	if(readSequence != 1234)
    		Log.e(TAG, "readSequence != 1234");
		Log.i(TAG, "[testIndexKey()] END");
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
