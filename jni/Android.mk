# File: Android.mk
LOCAL_PATH := $(call my-dir)
PARENT_LOCAL_PATH := $(wildcard ..)


include $(CLEAR_VARS)
LOCAL_MODULE := libcrypto
LOCAL_SRC_FILES := $(PARENT_LOCAL_PATH)/vendor/cbforest/vendor/openssl/libs/android/$(TARGET_ARCH_ABI)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE	:=	CouchbaseLiteJavaForestDB

FORESTDB_PATH   :=  $(PARENT_LOCAL_PATH)/vendor/cbforest/vendor/forestdb
SNAPPY_PATH     :=  $(PARENT_LOCAL_PATH)/vendor/cbforest/vendor/snappy
SQLITE3_PATH   	:=  $(PARENT_LOCAL_PATH)/vendor/cbforest/vendor/sqlite3-unicodesn
SQLITE_INC_PATH :=  $(PARENT_LOCAL_PATH)/vendor/sqlite
OPENSSL_PATH    :=  $(PARENT_LOCAL_PATH)/vendor/cbforest/vendor/openssl/libs/include
CBFOREST_PATH   :=  $(PARENT_LOCAL_PATH)/vendor/cbforest/CBForest
CBFOREST_C_PATH  :=  $(PARENT_LOCAL_PATH)/vendor/cbforest/C
CBFOREST_JAVA_PATH  :=  $(PARENT_LOCAL_PATH)/vendor/cbforest/Java
CBFOREST_JNI_PATH   :=  $(PARENT_LOCAL_PATH)/vendor/cbforest/Java/jni
FORESTDB_STORE_PATH :=  $(PARENT_LOCAL_PATH)/jni/source

LOCAL_CFLAGS    :=  -I$(SQLITE3_PATH)/libstemmer_c/runtime/ \
					-I$(SQLITE3_PATH)/libstemmer_c/src_c/ \
					-I$(SQLITE3_PATH)/ \
					-I$(SQLITE_INC_PATH)/ \
					-I$(OPENSSL_PATH)/

# For sqlite3-unicodesn
LOCAL_CFLAGS	+=	-DSQLITE_ENABLE_FTS4 \
					-DSQLITE_ENABLE_FTS4_UNICODE61 \
					-DWITH_STEMMER_english \
					-DDOC_COMP \
					-D_DOC_COMP \
					-DHAVE_GCC_ATOMICS=1 \
					-D_CRYPTO_OPENSSL

LOCAL_CPPFLAGS	:= 	-I$(FORESTDB_PATH)/include/ \
					-I$(FORESTDB_PATH)/include/libforestdb/ \
					-I$(FORESTDB_PATH)/src/ \
					-I$(FORESTDB_PATH)/utils/ \
					-I$(FORESTDB_PATH)/option/ \
					-I$(SNAPPY_PATH)/ \
					-I$(OPENSSL_PATH)/ \
					-I$(CBFOREST_PATH)/ \
					-I$(CBFOREST_C_PATH)/ \
					-I$(CBFOREST_JNI_PATH)/ \
					-I$(FORESTDB_STORE_PATH)/

ifeq ($(TARGET_ARCH),mips)
    LOCAL_CFLAGS +=	-D__mips32__
endif

LOCAL_CPPFLAGS	+=	-std=c++11
LOCAL_CPPFLAGS	+=	-fexceptions
LOCAL_CPPFLAGS	+=	-fpermissive
LOCAL_CPPFLAGS	+=	-frtti
LOCAL_CPPFLAGS	+=	-D__ANDROID__
LOCAL_CPPFLAGS	+=	-DC4DB_THREADSAFE
LOCAL_CPPFLAGS	+=	-DFORESTDB_VERSION=\"internal\"
#LOCAL_CPPFLAGS	+=	-DNO_CBFOREST_ENCRYPTION

# this requires for stdint.h active if android sdk is lower than or equal to android-19
# With android-21, it seems no longer necessary.
# http://stackoverflow.com/questions/986426/what-do-stdc-limit-macros-and-stdc-constant-macros-mean
LOCAL_CPPFLAGS	+=	-D__STDC_LIMIT_MACROS  
LOCAL_CPPFLAGS  +=  -g -O0
LOCAL_CPPFLAGS	+=	-Wno-unused-value
LOCAL_CPPFLAGS	+=	-Wno-deprecated-register
LOCAL_CPPFLAGS  +=  -fexceptions

LOCAL_CPP_FEATURES += rtti
LOCAL_CPP_FEATURES += exceptions

# 
PCH_FILE  := $(CBFOREST_PATH)/CBForest-Prefix.pch
LOCAL_CPPFLAGS += -include $(PCH_FILE)

LOCAL_LDLIBS    := -llog
LOCAL_LDLIBS    += -latomic

LOCAL_SRC_FILES :=	$(SQLITE3_PATH)/fts3_unicode2.c \
					$(SQLITE3_PATH)/fts3_unicodesn.c \
					$(SQLITE3_PATH)/libstemmer_c/runtime/api_sq3.c \
					$(SQLITE3_PATH)/libstemmer_c/runtime/utilities_sq3.c \
					$(SQLITE3_PATH)/libstemmer_c/libstemmer/libstemmer_utf8.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_danish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_dutch.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_english.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_finnish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_french.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_german.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_hungarian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_italian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_norwegian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_porter.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_portuguese.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_spanish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_1_swedish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_ISO_8859_2_romanian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_KOI8_R_russian.c \
                    $(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_danish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_dutch.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_english.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_finnish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_french.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_german.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_hungarian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_italian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_norwegian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_porter.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_portuguese.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_romanian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_russian.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_spanish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_swedish.c \
					$(SQLITE3_PATH)/libstemmer_c/src_c/stem_UTF_8_turkish.c \
                    $(FORESTDB_PATH)/utils/crc32.cc \
					$(FORESTDB_PATH)/utils/debug.cc \
					$(FORESTDB_PATH)/utils/iniparser.cc \
					$(FORESTDB_PATH)/utils/memleak.cc \
					$(FORESTDB_PATH)/utils/partiallock.cc \
					$(FORESTDB_PATH)/utils/system_resource_stats.cc \
					$(FORESTDB_PATH)/utils/time_utils.cc \
					$(FORESTDB_PATH)/src/api_wrapper.cc \
					$(FORESTDB_PATH)/src/avltree.cc \
					$(FORESTDB_PATH)/src/bgflusher.cc \
					$(FORESTDB_PATH)/src/blockcache.cc \
					$(FORESTDB_PATH)/src/breakpad_dummy.cc \
					$(FORESTDB_PATH)/src/btree.cc \
					$(FORESTDB_PATH)/src/btree_fast_str_kv.cc \
					$(FORESTDB_PATH)/src/btree_kv.cc \
					$(FORESTDB_PATH)/src/btree_str_kv.cc \
					$(FORESTDB_PATH)/src/btreeblock.cc \
					$(FORESTDB_PATH)/src/checksum.cc \
					$(FORESTDB_PATH)/src/compactor.cc \
					$(FORESTDB_PATH)/src/configuration.cc \
					$(FORESTDB_PATH)/src/docio.cc \
					$(FORESTDB_PATH)/src/encryption_aes.cc \
					$(FORESTDB_PATH)/src/encryption_bogus.cc \
					$(FORESTDB_PATH)/src/encryption.cc \
					$(FORESTDB_PATH)/src/fdb_errors.cc \
					$(FORESTDB_PATH)/src/filemgr.cc \
					$(FORESTDB_PATH)/src/filemgr_ops.cc \
					$(FORESTDB_PATH)/src/filemgr_ops_linux.cc \
					$(FORESTDB_PATH)/src/filemgr_ops_windows.cc \
					$(FORESTDB_PATH)/src/forestdb.cc \
					$(FORESTDB_PATH)/src/hash.cc \
					$(FORESTDB_PATH)/src/hash_functions.cc \
					$(FORESTDB_PATH)/src/hbtrie.cc \
					$(FORESTDB_PATH)/src/iterator.cc \
					$(FORESTDB_PATH)/src/kv_instance.cc \
					$(FORESTDB_PATH)/src/list.cc \
					$(FORESTDB_PATH)/src/staleblock.cc \
					$(FORESTDB_PATH)/src/superblock.cc \
					$(FORESTDB_PATH)/src/transaction.cc \
					$(FORESTDB_PATH)/src/version.cc \
					$(FORESTDB_PATH)/src/wal.cc \
					$(SNAPPY_PATH)/snappy.cc \
					$(SNAPPY_PATH)/snappy-c.cc \
					$(SNAPPY_PATH)/snappy-sinksource.cc \
					$(SNAPPY_PATH)/snappy-stubs-internal.cc \
					$(CBFOREST_PATH)/slice.cc \
					$(CBFOREST_PATH)/sqlite_glue.c \
					$(CBFOREST_PATH)/varint.cc \
					$(CBFOREST_PATH)/Collatable.cc \
					$(CBFOREST_PATH)/Database.cc \
					$(CBFOREST_PATH)/DocEnumerator.cc \
					$(CBFOREST_PATH)/Document.cc \
					$(CBFOREST_PATH)/Error.cc \
					$(CBFOREST_PATH)/FullTextIndex.cc \
					$(CBFOREST_PATH)/Geohash.cc \
					$(CBFOREST_PATH)/GeoIndex.cc \
					$(CBFOREST_PATH)/Index.cc \
					$(CBFOREST_PATH)/KeyStore.cc \
					$(CBFOREST_PATH)/RevID.cc \
					$(CBFOREST_PATH)/RevTree.cc \
					$(CBFOREST_PATH)/VersionedDocument.cc \
					$(CBFOREST_PATH)/MapReduceIndex.cc \
					$(CBFOREST_PATH)/Tokenizer.cc \
					$(CBFOREST_C_PATH)/c4.c \
					$(CBFOREST_C_PATH)/c4Database.cc \
					$(CBFOREST_C_PATH)/c4DocEnumerator.cc \
					$(CBFOREST_C_PATH)/c4Document.cc \
					$(CBFOREST_C_PATH)/c4ExpiryEnumerator.cc \
					$(CBFOREST_C_PATH)/c4Key.cc \
					$(CBFOREST_C_PATH)/c4View.cc \
					$(CBFOREST_JNI_PATH)/native_database.cc \
					$(CBFOREST_JNI_PATH)/native_document.cc \
					$(CBFOREST_JNI_PATH)/native_documentiterator.cc \
					$(CBFOREST_JNI_PATH)/native_glue.cc \
					$(CBFOREST_JNI_PATH)/native_indexer.cc \
					$(CBFOREST_JNI_PATH)/native_queryIterator.cc \
					$(CBFOREST_JNI_PATH)/native_view.cc \
					$(FORESTDB_STORE_PATH)/native_forestdbstore.cc

LOCAL_STATIC_LIBRARIES := libcrypto
					
include $(BUILD_SHARED_LIBRARY)