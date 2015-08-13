#!/bin/sh

#cp cbforest.jar ~/github/couchbase-lite-android/libraries/couchbase-lite-java-core/libs/
#cp -r libs/* ~/github/couchbase-lite-android/jniLibs/


#cp -r src/   ~/github/cbforest-java-test/app/src/main/java
rm -rf ./test/cbforest-java-test/app/jniLibs/*
rm -f  ./test/cbforest-java-test/app/libs/cbforest.jar
cp cbforest.jar ./test/cbforest-java-test/app/libs/
cp -r libs/*    ./test/cbforest-java-test/app/jniLibs/