
Java wrapper around [CBForest](https://github.com/couchbaselabs/cbforest).  Includes SWIG generated java wrappers for CBForest.

The [feature/cbforest](https://github.com/couchbase/couchbase-lite-android/tree/feature/cbforest) branch of [couchbase-lite-android](https://github.com/couchbase/couchbase-lite-android) is using cbforest-java.

## Quick Start

```
$ git clone <repo>
$ git submodule update --init --recursive
```

### Prerequisites
* Install [SWIG](http://www.swig.org/)

If you are using Mac OSX
```
$ brew install swig
```
* Install [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html)

Note: Please add NDK home directory in the envronment PATH
```
#export ANDROID_NDK_HOME=<NDK home directory>
#export PATH=$ANDROID_NDK_HOME:$PATH
```

### Generate JNI java and native (C/C++) binding codes by SWIG
```
$ make swig
```
### Compile JNI native (C/C++) codes by Android NDK
Note: Update Makefile to specify your NDK build command path
```
$ make ndk-build
```
### Compile JNI java files and make jar file
```
$ make jar
```

### Outcome 
* cbforest.jar
* libs/[platform]/libcbforest.so
