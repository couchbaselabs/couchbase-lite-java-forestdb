all: clean swig ndk-build jar



JNI_SRC_DIR=./jni
JNI_JAVA_PACKAGE_DIR=./src/main/java/com/couchbase/lite/cbforest
JNI_JAVA_PACKAGE=com.couchbase.lite.cbforest

clean:
	rm -rf $(JNI_JAVA_PACKAGE_DIR)
	rm -rf $(JNI_SRC_DIR)/cbforest_wrap.h
	rm -rf $(JNI_SRC_DIR)/cbforest_wrap.cc
	rm -rf libs
	rm -rf obj
	rm -rf classes
	rm -rf cbforest.jar


# SWIG: Generate JNI java binding codes and Native (C/C++) binding codes fro cbforest.i and C/C++ header files
swig:
	rm -rf $(JNI_SRC_DIR)/cbforest_wrap.cc
	rm -rf $(JNI_JAVA_PACKAGE_DIR)
	mkdir -p $(JNI_JAVA_PACKAGE_DIR)
	swig -Wall -c++ -java \
		-I./vendor/cbforest/vendor/forestdb/include \
		-I./vendor/cbforest/vendor/forestdb/include/libforestdb \
		-I./vendor/cbforest/CBForest \
		-package $(JNI_JAVA_PACKAGE) \
		-outdir $(JNI_JAVA_PACKAGE_DIR) \
		-o $(JNI_SRC_DIR)/cbforest_wrap.cc \
		$(JNI_SRC_DIR)/cbforest.i

# Build native liberary by NDK
# NOTE: please modify ndk-build command path below!!
ndk-build:
	ndk-build -C jni clean
	ndk-build -C jni

# compile JNI java binding code and make Jar file
jar:
	mkdir -p ./classes
	javac -source 1.7 -target 1.7 -d ./classes ./src/com/couchbase/lite/cbforest/*.java
	jar -cf cbforest.jar -C classes/ .