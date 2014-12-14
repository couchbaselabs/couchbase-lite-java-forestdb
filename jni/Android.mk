# File: Android.mk
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE	:=	cbforest

FORESTDB_PATH   :=  $(LOCAL_PATH)/../vendor/cbforest/vendor/forestdb
CBFOREST_PATH   :=  $(LOCAL_PATH)/../vendor/cbforest/CBForest

LOCAL_CPPFLAGS	:= 	-I$(FORESTDB_PATH)/include/ \
					-I$(FORESTDB_PATH)/include/libforestdb/ \
					-I$(FORESTDB_PATH)/src/ \
					-I$(FORESTDB_PATH)/utils/ \
					-I$(FORESTDB_PATH)/option/ \
					-I$(CBFOREST_PATH)/

LOCAL_CPPFLAGS	+=	-std=c++11
LOCAL_CPPFLAGS	+=	-fexceptions
LOCAL_CPPFLAGS	+=	-fpermissive
LOCAL_CPPFLAGS	+=	-frtti
LOCAL_CPPFLAGS	+=	-D__ANDROID__

# this requires for stdint.h active if android sdk is lower than or equal to android-19
# With android-21, it seems no longer necessary.
# http://stackoverflow.com/questions/986426/what-do-stdc-limit-macros-and-stdc-constant-macros-mean
LOCAL_CPPFLAGS	+=	-D__STDC_LIMIT_MACROS  
#LOCAL_CPPFLAGS  +=  -g -O0
LOCAL_CPPFLAGS	+=	-Wno-unused-value
LOCAL_CPPFLAGS	+=	-Wno-deprecated-register
LOCAL_CPPFLAGS  += -fexceptions

LOCAL_CPP_FEATURES += rtti
LOCAL_CPP_FEATURES += exceptions

# 
PCH_FILE  := $(CBFOREST_PATH)/CBForest-Prefix.pch
LOCAL_CPPFLAGS += -include $(PCH_FILE)

LOCAL_LDLIBS    := -llog

LOCAL_SRC_FILES :=	$(FORESTDB_PATH)/utils/adler32.cc \
					$(FORESTDB_PATH)/utils/cJSON.cc \
					$(FORESTDB_PATH)/utils/crc32.cc \
					$(FORESTDB_PATH)/utils/debug.cc \
					$(FORESTDB_PATH)/utils/memleak.cc \
					$(FORESTDB_PATH)/src/api_wrapper.cc \
					$(FORESTDB_PATH)/src/avltree.cc \
					$(FORESTDB_PATH)/src/blockcache.cc \
					$(FORESTDB_PATH)/src/btree.cc \
					$(FORESTDB_PATH)/src/btree_fast_str_kv.cc \
					$(FORESTDB_PATH)/src/btree_kv.cc \
					$(FORESTDB_PATH)/src/btree_prefix_kv.cc \
					$(FORESTDB_PATH)/src/btree_str_kv.cc \
					$(FORESTDB_PATH)/src/btreeblock.cc \
					$(FORESTDB_PATH)/src/compactor.cc \
					$(FORESTDB_PATH)/src/configuration.cc \
					$(FORESTDB_PATH)/src/docio.cc \
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
					$(FORESTDB_PATH)/src/snapshot.cc \
					$(FORESTDB_PATH)/src/transaction.cc \
					$(FORESTDB_PATH)/src/wal.cc \
					$(CBFOREST_PATH)/slice.cc \
					$(CBFOREST_PATH)/varint.cc \
					$(CBFOREST_PATH)/Collatable.cc \
					$(CBFOREST_PATH)/Database.cc \
					$(CBFOREST_PATH)/DocEnumerator.cc \
					$(CBFOREST_PATH)/Document.cc \
					$(CBFOREST_PATH)/Index.cc \
					$(CBFOREST_PATH)/KeyStore.cc \
					$(CBFOREST_PATH)/RevID.cc \
					$(CBFOREST_PATH)/RevTree.cc \
					$(CBFOREST_PATH)/VersionedDocument.cc \
					$(CBFOREST_PATH)/MapReduceIndex.cc \
					cbf_collatable.cc \
					cbf_keystore.cc \
					cbf_database.cc \
					cbf_docenumerator.cc \
					cbf_document.cc \
					cbf_index.cc \
					cbf_mapreduceindex.cc \
					cbf_revid.cc \
					cbf_revtree.cc \
					cbf_slice.cc \
					cbf_versioneddocument.cc \
					cbf.cc \
					cbforest_wrap.cc
					
include $(BUILD_SHARED_LIBRARY)