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

public class C4EncryptedDatabaseTest extends C4DatabaseTest{

    //static final int algorithm = -1; // FAKE encryption
    static final int  algorithm = Database.AES256Encryption;
    /**
     * For now, the AES encryption mode (kC4EncryptionAES) is only implemented for Apple platforms.
     * It should be easy to hook it up to other platforms’ native crypto APIs, though; see forestdb/utils/crypto_primitives.h.
     *
     * For testing, there is a hidden ‘encryption' mode whose constant is -1.
     * It’s a trivial and really insecure XOR-based algorithm, but it’ll validate that the right key is being used.
     */
    static final byte[] encryptionKey = "this is not a random key at all...".substring(0, 32).getBytes();
    // original key length is 34. It is reason to substirng to 0..32.

    protected int encryptionAlgorithm() {
        return algorithm;
    }

    protected byte[] encryptionKey() {
        return encryptionKey;
    }

    public void testRekey() throws ForestException {
        testCreateRawDoc();

        db.rekey(0, null);

        final String store = "test";
        String key = "key";
        byte[][] metaNbody = db.rawGet(store, key);
        assertNotNull(metaNbody);
        assertEquals(2, metaNbody.length);
    }

}
