/** 
 * cbforest.i  - SWIG interface
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */

%module(directors="1") cbforest

// for uint8, uint16, uint32 and uint64
%include "stdint.i"
%include "std_string.i"
%include "std_shared_ptr.i"
%include "std_vector.i"
%include "typemaps.i"
%include "various.i"

// ignore classes & methods
%ignore CBF::Slice::Slice(const forestdb::slice& );
%ignore CBF::Config::Config(const fdb_config&);
%ignore CBF::KvsInfo::KvsInfo(const fdb_kvs_info&);
%ignore CBF::FileInfo::FileInfo(const fdb_file_info&);
%ignore CBF::Document::Document(const forestdb::Document&);
%ignore CBF::Database::Database(forestdb::Database*);
%ignore CBF::RevID::RevID(const forestdb::slice&);
%ignore CBF::RevID::RevID(const forestdb::revid&);
%ignore CBF::Revision::Revision(const forestdb::Revision*);
%ignore CBF::RevIDBuffer::RevIDBuffer(const forestdb::revidBuffer&);
%ignore CBF::RevTree::RevTree(const forestdb::RevTree*);
%ignore CBF::Collatable::Collatable(const forestdb::Collatable&);
%ignore CBF::CollatableReader::CollatableReader(const forestdb::CollatableReader&);
%ignore CBF::EmitFn::EmitFn(forestdb::EmitFn*);
%ignore CBF::MapFn::operator() (const forestdb::Mappable&, forestdb::EmitFn&);
%ignore CBF::BridgeMapReduceIndexer;
%ignore CBF::MapReduceIndexer::InnerMapReduceIndexer;
%ignore CBF::MapReduceIndexer::bridgeAddDocument(const forestdb::Document&);

%ignoreoperator(EQ) operator=;

%{
	#include "cbf_forestdb.h"
	#include "cbf_slice.h"
	#include "cbf_keystore.h"
	#include "cbf_document.h"
	#include "cbf_database.h"
	#include "cbf_docenumerator.h"
	#include "cbf_revid.h"
	#include "cbf_revtree.h"
	#include "cbf_versioneddocument.h"
	#include "cbf_collatable.h"
	#include "cbf_index.h"
	#include "cbf_mapreduceindex.h"
%}

// For cbf_slice.h (Slice)
// Input parameters: const char*, size_t => byte[]
// SWIG and Java: 25.8.5 Binary data vs Strings
%apply (char *STRING, size_t LENGTH) { (const char* b, size_t s) }

// change Slice getBuf SWIG mapping of char* return value to byte[]
// --change default SWIG mapping of char* return values to byte[]
%typemap(jni)    char* getBuf    "jbyteArray"
%typemap(jtype)  char* getBuf    "byte[]"
%typemap(jstype) char* getBuf    "byte[]"
%typemap(out) char* CBF::Slice::getBuf {
  if(result == NULL) return NULL;
  size_t size = (arg1)->getSize();
  $result = JCALL1(NewByteArray, jenv, size);
  JCALL4(SetByteArrayRegion, jenv, $result, 0, size, (const jbyte*)result);
}
%typemap(out) char* CBF::RevID::getBuf {
  if(result == NULL) return NULL;
  size_t size = strlen(result);
  $result = JCALL1(NewByteArray, jenv, size);
  JCALL4(SetByteArrayRegion, jenv, $result, 0, size, (const jbyte*)result);
}

// for vector
%template(VectorString)         std::vector<std::string>;
%template(VectorRevision)       std::vector<CBF::Revision*>;
%template(VectorCollatable)     std::vector<CBF::Collatable>;
%template(VectorMapReduceIndex) std::vector<CBF::MapReduceIndex*>;
%template(VectorKeyRange)       std::vector<CBF::KeyRange*>;

/**
 * Mappable 
 * NOTE: Mappable is extended Java side, and is passed as parameter from native to java.
 *       But SWIG does not support this. Following is workaround.
 *       See: http://stackoverflow.com/questions/9817516/swig-java-retaining-class-information-of-the-objects-bouncing-from-c
 *
 * TODO: Revisit this!!!!
 */  

// An import for the hashmap type
%typemap(javaimports) CBF::Mappable %{
import java.util.HashMap;
import java.lang.ref.WeakReference;
%}

// Provide a static hashmap, 
// replace the constructor to add to it for derived Java types
%typemap(javabody) CBF::Mappable %{
  private static HashMap<Long, WeakReference<$javaclassname>> instances 
                        = new HashMap<Long, WeakReference<$javaclassname>>();

  private long swigCPtr;
  protected boolean swigCMemOwn;

  public $javaclassname(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
    // If derived add it.
    if (getClass() != $javaclassname.class) {
      instances.put(swigCPtr, new WeakReference<$javaclassname>(this));
    }
  }

  // Just the default one
  public static long getCPtr($javaclassname obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  // Helper function that looks up given a pointer and 
  // either creates or returns it
  static $javaclassname createOrLookup(long arg) {
    if (instances.containsKey(arg)) {
      return instances.get(arg).get();
    }
    return new $javaclassname(arg,false);
  }
%}

// Remove from the map when we release the C++ memory
%typemap(javadestruct, methodname="delete", 
         methodmodifiers="public synchronized") CBF::Mappable {
  if (swigCPtr != 0) {
    // Unregister instance
    instances.remove(swigCPtr);
    if (swigCMemOwn) {
      swigCMemOwn = false;
      $imclassname.delete_Mappable(swigCPtr);
    }
    swigCPtr = 0;
  }
}

// Tell SWIG to use the createOrLookup function in director calls.
%typemap(javadirectorin) CBF::Mappable& %{
    $javaclassname.createOrLookup($jniinput)
%}

/**
 * End of Mappable
 */

// for virtual function
// %feature("director") CBF::Mappable;
%feature("director") CBF::MapFn;
%feature("director") CBF::MapReduceIndexer;


// Parse the original header file
%include "cbf_forestdb.h"
%include "cbf_slice.h"
%include "cbf_keystore.h"
%include "cbf_document.h"
%include "cbf_database.h"
%include "cbf_docenumerator.h"
%include "cbf_revid.h"
%include "cbf_revtree.h"
%include "cbf_versioneddocument.h"
%include "cbf_collatable.h"
%include "cbf_index.h"
%include "cbf_mapreduceindex.h"












