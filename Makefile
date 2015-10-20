all: clean javah ndk-build jar

JAVA_SRC_DIR=./vendor/cbforest/Java/src
JNI_SRC_DIR=./vendor/cbforest/Java/jni

clean:
	rm -rf $(JNI_SRC_DIR)/com_couchbase_cbforest_*.h
	rm -rf libs
	rm -rf obj
	rm -rf classes
	rm -rf cbforest.jar

javah:
	javah -classpath $(JAVA_SRC_DIR) -d $(JNI_SRC_DIR) \
				com.couchbase.cbforest.Database \
				com.couchbase.cbforest.Document \
				com.couchbase.cbforest.DocumentIterator \
				com.couchbase.cbforest.ForestException \
				com.couchbase.cbforest.Logger \
				com.couchbase.cbforest.QueryIterator \
				com.couchbase.cbforest.View

# Build native liberary by NDK
# NOTE: please modify ndk-build command path below!!
ndk-build:
	ndk-build -C jni clean
	ndk-build -C jni 

# compile JNI java binding code and make Jar file
jar:
	mkdir -p ./classes
	javac -source 1.7 -target 1.7 -d ./classes $(JAVA_SRC_DIR)/com/couchbase/cbforest/*.java
	jar -cf cbforest.jar -C classes/ .