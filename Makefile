all: clean swig ndk-build jar

clean:
	rm -rf src
	rm -rf jni/cbforest_wrap.h
	rm -rf jni/cbforest_wrap.cc
	rm -rf libs
	rm -rf obj
	rm -rf classes
	rm -rf cbforest.jar


# SWIG: Generate JNI java binding codes and Native (C/C++) binding codes fro cbforest.i and C/C++ header files
swig:
	rm -rf src/com/couchbase/lite/cbforest
	mkdir -p src/com/couchbase/lite/cbforest
	swig -Wall -c++ -java \
		-I./vendor/cbforest/vendor/forestdb/include \
		-I./vendor/cbforest/vendor/forestdb/include/libforestdb \
		-I./vendor/cbforest/CBForest \
		-package com.couchbase.lite.cbforest \
		-outdir ./src/com/couchbase/lite/cbforest \
		-o ./jni/cbforest_wrap.cc \
		./jni/cbforest.i

# Build native liberary by NDK
# NOTE: please modify ndk-build command path below!!
ndk-build:
	ndk-build -C jni clean
	ndk-build -C jni

# compile JNI java binding code and make Jar file
jar:
	mkdir -p ./classes
	javac -d ./classes ./src/com/couchbase/lite/cbforest/*.java
	jar -cf cbforest.jar -C classes/ .