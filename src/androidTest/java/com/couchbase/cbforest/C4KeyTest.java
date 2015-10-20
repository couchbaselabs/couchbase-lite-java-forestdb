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

public class C4KeyTest extends C4TestCase implements Constants {
    private long _key = 0;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        _key = View.newKey();
    }

    @Override
    protected void tearDown() throws Exception {
        if(_key !=0) {
            View.freeKey(_key);
            _key = 0;
        }
        super.tearDown();
    }

    void populateKey() {
        View.keyBeginArray(_key);
        View.keyAddNull(_key);
        View.keyAdd(_key, false);
        View.keyAdd(_key, true);
        View.keyAdd(_key, 0);
        View.keyAdd(_key, 12345);
        View.keyAdd(_key, -2468);
        View.keyAdd(_key, "foo");
        View.keyBeginArray(_key);
        View.keyEndArray(_key);
        View.keyEndArray(_key);
    }

    public void testCreateKey() {
        populateKey();

        assertEquals("[null,false,true,0,12345,-2468,\"foo\",[]]", View.keyToJSON(_key));
    }

    public void testReadKey() {
        populateKey();

        long _reader = View.keyReader(_key);
        try {
            assertEquals(C4KeyToken.kC4Array, View.keyPeek(_reader));
            View.keySkipToken(_reader);
            assertEquals(C4KeyToken.kC4Null, View.keyPeek(_reader));
            View.keySkipToken(_reader);
            assertEquals(C4KeyToken.kC4Bool, View.keyPeek(_reader));
            assertEquals(false, View.keyReadBool(_reader));
            assertEquals(true, View.keyReadBool(_reader));
            assertEquals(0.0, View.keyReadNumber(_reader));
            assertEquals(12345.0, View.keyReadNumber(_reader));
            assertEquals(-2468.0, View.keyReadNumber(_reader));
            assertEquals("foo", View.keyReadString(_reader));
            assertEquals(C4KeyToken.kC4Array, View.keyPeek(_reader));
            View.keySkipToken(_reader);
            assertEquals(C4KeyToken.kC4EndSequence, View.keyPeek(_reader));
            View.keySkipToken(_reader);
            assertEquals(C4KeyToken.kC4EndSequence, View.keyPeek(_reader));
            View.keySkipToken(_reader);
        }finally {
            View.freeKeyReader(_reader);
            _reader = 0;
        }
    }
}
