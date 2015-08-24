/*
 * cbf_slice.cc
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */

#include "slice.hh"
#include "cbf_slice.h"

namespace CBF {

/**
 * Slice - implementation
 */

Slice::Slice() {
	_slice = new forestdb::slice();
}

Slice::Slice(const char* b, size_t s) {
	init(b, s);
}

Slice::Slice(const forestdb::slice& s) {
	init((const char *) s.buf, s.size);
}

Slice::~Slice() {
	releaseData();
}

// Return a pointer to the beginning of the referenced data
char* Slice::getBuf() {
	if (_slice == NULL)
		return NULL;
	return (char *) _slice->buf;
}

int Slice::compare(Slice& other) {
	if (_slice == NULL && other._slice == NULL)
		return 0;
	if (_slice == NULL && other._slice != NULL)
		return -1;
	if (_slice != NULL && other._slice == NULL)
		return 1;
	return _slice->compare(*other._slice);
}

Slice* Slice::copy(){
	return new Slice((const char*)_slice->buf, _slice->size);
}

void Slice::free(){
	_slice->free();
}

// Return the length (in bytes) of the referenced data
const int Slice::getSize() {
	return (const int)_slice->size;
}
const void* Slice::getData(){
	return _slice->buf;
}

void Slice::releaseData() {
	if (_slice != NULL) {
		if (_slice->buf != NULL) {
			::free((void*)_slice->buf);
			_slice->buf = NULL;
			_slice->size = 0;
		}
		delete _slice;
		_slice = NULL;
	}
}

void Slice::init(const char* b, size_t s) {
	if(b == NULL){
		_slice = new forestdb::slice((void *)NULL, (unsigned int)s);
	}
	else{
		//char* tmp = new char[s + 1];
		char* tmp = (char*)::malloc(s);
		if (tmp != NULL) {
			::memcpy(tmp, b, s);
			_slice = new forestdb::slice(tmp, s);
		}
	}
}

}
