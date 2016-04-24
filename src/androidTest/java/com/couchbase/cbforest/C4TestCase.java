/**
 * Created by Hideki Itakura on 10/20/2015.
 * Copyright (c) 2015 Couchbase, Inc All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.couchbase.cbforest;

import android.test.AndroidTestCase;

import com.couchbase.lite.util.Log;

import java.io.File;

/**
 * Ported from c4Test.cc
 */
public class C4TestCase extends AndroidTestCase implements Constants{
    public static final String TAG = C4TestCase.class.getSimpleName();

    public static final String DB_FILENAME = "forest_temp.fdb";

    static {
        try {
            System.loadLibrary("CouchbaseLiteJavaForestDB");
        } catch (Exception e) {
            Log.e(TAG, "ERROR: Failed to load libCouchbaseLiteJavaForestDB");
            fail("ERROR: Failed to load libCouchbaseLiteJavaForestDB.");
        }
    }

    protected int encryptionAlgorithm() {
        return Database.NoEncryption;
    }

    protected byte[] encryptionKey() {
        return null;
    }

    protected Database db = null;
    protected static final String kDocID = "mydoc";
    protected static final String kRevID = "1-abcdef";
    protected static final String kRev2ID= "2-d00d3333";
    protected static final String kBody  = "{\"name\":007}";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteDatabaseFile(DB_FILENAME);
        File dbFile = new File(mContext.getFilesDir(), DB_FILENAME);
        db = new Database(dbFile.getPath(), Database.Create, encryptionAlgorithm(), encryptionKey());
    }

    protected void deleteDatabaseFile(String dbFileName) {
        deleteFile(dbFileName);
        deleteFile(dbFileName + ".0");
        deleteFile(dbFileName + ".1");
        deleteFile(dbFileName + ".meta");
    }

    private void deleteFile(String filename){
        File file = new File(mContext.getFilesDir(), filename);
        if (file.exists()) {
            if (!file.delete()) {
                Log.e(TAG, "ERROR failed to delete: dbFile=" + file);
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {

        if (db != null) {
            db.close();
            db = null;
        }

        super.tearDown();
    }

    protected void createRev(String docID, String revID, byte[] body) throws ForestException{
        createRev(docID, revID, body, true); // 1 for new
    }

    /**
     * @param isNew true - new (201), false - not new (200)
     */
    protected void createRev(String docID, String revID, byte[] body, boolean isNew) throws ForestException{
        boolean commit = false;
        db.beginTransaction();
        try {
            Document doc = db.getDocument(docID, false);
            assertNotNull(doc);
            boolean deleted = body == null;
            assertEquals(isNew, doc.insertRevision(revID, body, deleted, false, false));
            doc.save(20);
            doc.free();
            commit = true;
        }finally {
            db.endTransaction(commit);
        }
    }
}
