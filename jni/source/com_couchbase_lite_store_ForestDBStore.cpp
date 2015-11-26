#include <stdio.h>

#include "com_couchbase_lite_store_ForestDBStore.h"


#if !defined (CBL_KEY_CRYPTO_CC) \
&& !defined (CBL_KEY_CRYPTO_OPENSSL)
#define CBL_KEY_CRYPTO_OPENSSL
#endif

#if defined (CBL_KEY_CRYPTO_CC)

#import <CommonCrypto/CommonCrypto.h>

JNIEXPORT jbyteArray JNICALL Java_com_couchbase_lite_store_ForestDBStore_nativeDerivePBKDF2SHA256Key
(JNIEnv* env, jclass clazz, jstring password, jbyteArray salt, jint rounds) {
    if (password == NULL || salt == NULL)
        return NULL;
    
    // Password:
    const char* passwordCStr = env->GetStringUTFChars(password, NULL);
    int passwordSize = (int)env->GetStringLength (password);
    
    // Salt:
    int saltSize = env->GetArrayLength (salt);
    unsigned char* saltBytes = new unsigned char[saltSize];
    env->GetByteArrayRegion (salt, 0, saltSize, reinterpret_cast<jbyte*>(saltBytes));
    
    // PBKDF2-SHA256
    int outputSize = 32; //256 bit
    unsigned char* output = new unsigned char[outputSize * 2];
    int status = CCKeyDerivationPBKDF(kCCPBKDF2,
                                      passwordCStr, passwordSize,
                                      saltBytes, saltSize,
                                      kCCPRFHmacAlgSHA256, rounds,
                                      output, outputSize);
    
    // Release memory:
    env->ReleaseStringUTFChars(password, passwordCStr);
    delete[] saltBytes;
    
    // Return null if not success:
    if (status)
        return NULL;
    
    // Result:
    jbyteArray result = env->NewByteArray(outputSize);
    env->SetByteArrayRegion(result, 0, outputSize, (jbyte*)output);
    
    // Release memory:
    delete[] output;
    
    return result;
}

#elif defined (CBL_KEY_CRYPTO_OPENSSL)

#include "openssl/evp.h"
#include "openssl/sha.h"

JNIEXPORT jbyteArray JNICALL Java_com_couchbase_lite_store_ForestDBStore_nativeDerivePBKDF2SHA256Key
(JNIEnv* env, jclass clazz, jstring password, jbyteArray salt, jint rounds)  {
    if (password == NULL || salt == NULL)
        return NULL;
    
    // Password:
    const char* passwordCStr = env->GetStringUTFChars(password, NULL);
    int passwordSize = (int)env->GetStringLength (password);
    
    // Salt:
    int saltSize = env->GetArrayLength (salt);
    unsigned char* saltBytes = new unsigned char[saltSize];
    env->GetByteArrayRegion (salt, 0, saltSize, reinterpret_cast<jbyte*>(saltBytes));
    
    // PBKDF2-SHA256
    int outputSize = 32; //256 bit
    unsigned char* output = new unsigned char[outputSize * 2];
    int status = PKCS5_PBKDF2_HMAC(passwordCStr, passwordSize, saltBytes, saltSize,
                                   (int)rounds, EVP_sha256(), outputSize, output);
    // Release memory:
    env->ReleaseStringUTFChars(password, passwordCStr);
    delete[] saltBytes;
    
    // Return null if not success:
    if (status == 0)
        return NULL;
    
    // Result:
    jbyteArray result = env->NewByteArray(outputSize);
    env->SetByteArrayRegion(result, 0, outputSize, (jbyte*)output);
    
    // Release memory:
    delete[] output;
    
    return result;
}

#else
#error "NO DEFAULT CRYPTO PROVIDER DEFINED"
#endif
