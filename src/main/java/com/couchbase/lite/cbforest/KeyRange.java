/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.7
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.couchbase.lite.cbforest;

public class KeyRange {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected KeyRange(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(KeyRange obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        cbforestJNI.delete_KeyRange(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public KeyRange(Collatable s, Collatable e, boolean inclusive) {
    this(cbforestJNI.new_KeyRange__SWIG_0(Collatable.getCPtr(s), s, Collatable.getCPtr(e), e, inclusive), true);
  }

  public KeyRange(Collatable s, Collatable e) {
    this(cbforestJNI.new_KeyRange__SWIG_1(Collatable.getCPtr(s), s, Collatable.getCPtr(e), e), true);
  }

  public KeyRange(Collatable arg0) {
    this(cbforestJNI.new_KeyRange__SWIG_2(Collatable.getCPtr(arg0), arg0), true);
  }

  public KeyRange(KeyRange arg0) {
    this(cbforestJNI.new_KeyRange__SWIG_3(KeyRange.getCPtr(arg0), arg0), true);
  }

  public boolean isKeyPastEnd(Slice arg0) {
    return cbforestJNI.KeyRange_isKeyPastEnd(swigCPtr, this, Slice.getCPtr(arg0), arg0);
  }

}