package com.couchbase.lite.cbforest;

import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;

/**
 * Created by hideki on 8/12/15.
 */
public class BaseCBForestTestCase extends AndroidTestCase {

    public static final String TAG = BaseCBForestTestCase.class.getSimpleName();

    /** static constructor */
    static {
        try{
            System.loadLibrary("cbforest");
        }
        catch(Exception e){
            Log.e(TAG, "ERROR: Failed to load libcbforest !!!");
            fail("ERROR: Failed to load libcbforest.");
        }
    }

    public static final String dbfilename = "forest_temp.fdb";

    Database db = null;

    @Override
    protected void setUp() throws Exception {

        super.setUp();
        File dbFile = new File(mContext.getFilesDir(), dbfilename);
        Log.i(TAG, "dbFile=" + dbFile);
        if (dbFile.exists()) {
            if (!dbFile.delete()) {
                Log.e(TAG, "ERROR failed to delete: dbFile=" + dbFile);
            }
        }
        Log.i(TAG, "[setUp()] call Database()");
        db = new Database(dbFile.getPath(), Database.defaultConfig());
        Log.i(TAG, "[setUp()] db => " + db);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (db != null) {
            db.delete();
            db = null;
        }
    }
}
