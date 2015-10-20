package com.couchbase.cbforest;

/**
 * Created by hideki on 10/8/15.
 */
public class C4EncryptedDatabaseTest extends C4DatabaseTest{

    static final int algorithm = -1; /* Database.AES256Encryption; */
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
