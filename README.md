
Java wrapper around [CBForest](https://github.com/couchbaselabs/cbforest).  Includes SWIG generated java wrappers for CBForest.

The [feature/cbforest](https://github.com/couchbase/couchbase-lite-android/tree/feature/cbforest) branch of [couchbase-lite-android](https://github.com/couchbase/couchbase-lite-android) is using cbforest-java.

## Quick Start

```
$ git clone <repo>
$ cd cbforest-java
$ git submodule update --init --recursive
```
#### Prerequisites
* Install [SWIG](http://www.swig.org/)  -- Optional

If you are using Mac OSX
```
$ brew install swig
```
* Install [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html) version r10c

Note: Please add ANDROID SDK and NDK home directories in the envronment PATH
```
export ANDROID_HOME=<Android SDK home directory>
export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH

#export ANDROID_NDK_HOME=<NDK home directory>
#export PATH=$ANDROID_NDK_HOME:$PATH
```

### Build by Gradle

#### How to generate the AAR file
```
$ ./gradlew assemble
$ cd build/outputs/aar
```
#### Run UnitTest
```
$ ./gradlew connectedAndroidTest --debug
```

### Build by make

#### Generate JNI java and native (C/C++) binding codes by SWIG
```
$ make swig
```
#### Compile JNI native (C/C++) codes by Android NDK
Note: Update Makefile to specify your NDK build command path
```
$ make ndk-build
```
#### Compile JNI java files and make jar file
```
$ make jar
```

#### Outcome 
* cbforest.jar
* libs/[platform]/libcbforest.so
