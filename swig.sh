#!/bin/sh

rm   -rf src/com/couchbase/lite/cbforest
mkdir -p src/com/couchbase/lite/cbforest

swig -Wall -c++ -java \
	-I./vendor/cbforest/vendor/forestdb/include \
	-I./vendor/cbforest/vendor/forestdb/include/libforestdb \
	-I./vendor/cbforest/CBForest \
	-package com.couchbase.lite.cbforest \
	-outdir ./src/com/couchbase/lite/cbforest \
	-o ./jni/cbforest_wrap.cc \
	./jni/cbforest.i
