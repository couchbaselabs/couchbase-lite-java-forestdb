#!/bin/sh

~/java/android-ndk-r10c/ndk-build clean
~/java/android-ndk-r10c/ndk-build

cp -r ../libs/* ~/github/couchbase-lite-android/jniLibs
mkdir -p ~/github/couchbase-lite-android/libraries/couchbase-lite-java-core/src/main/java/com/couchbase/lite/cbforest
rm -f ~/github/couchbase-lite-android/libraries/couchbase-lite-java-core/src/main/java/com/couchbase/lite/cbforest/*.java
cp -r ../src/com/couchbase/lite/cbforest/* ~/github/couchbase-lite-android/libraries/couchbase-lite-java-core/src/main/java/com/couchbase/lite/cbforest/