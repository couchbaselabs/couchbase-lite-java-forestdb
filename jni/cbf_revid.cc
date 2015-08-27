/*
 * cbf_revid.cc
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */

#include "slice.hh"
#include "cbf_revid.h"

#include <string.h>
#include <android/log.h>

namespace CBF {

/**
 * RevID - implementation
 */
RevID::RevID() {
	_slice = _revid = new forestdb::revid();
	buffer = NULL;
}

RevID::RevID(const char* b, size_t s) {
	init(b, s);
}

RevID::RevID(const char* str) {
	init(str, strlen(str));
}

RevID::RevID(const forestdb::revid& s) {
	init((const char *) s.buf, s.size);
}

RevID::RevID(const forestdb::slice& s) {
	init((const char *) s.buf, s.size);
}

RevID::~RevID() {
	releaseData();
}

bool RevID::isCompressed() const {
	return _revid->isCompressed();
}

Slice *RevID::expanded() const {
	return new Slice(_revid->expanded());
}

size_t RevID::expandedSize() const {
	return _revid->expandedSize();
}

bool RevID::expandInto(Slice &dst) const {
	return _revid->expandInto(*dst._slice);
}

unsigned RevID::generation() const {
	return _revid->generation();
}

Slice* RevID::digest() const {
	return new Slice(_revid->digest());
}

char* RevID::getBuf() {
	if(_revid->buf == NULL || _revid->size == 0)
		return NULL;

	Slice* slice = expanded();
	bufSize = slice->getSize();
	
	buffer = (char*)malloc(slice->getSize()+1);
	memset(buffer, 0, slice->getSize()+1);
	memcpy(buffer, slice->getBuf(), slice->getSize());

	return buffer;
}

void RevID::init(const char* b, size_t s) {
	if (b == NULL) {
		_revid = new forestdb::revid(NULL, 0);
	} else {
		void* tmp = ::malloc(s);
		if (tmp != NULL) {
			memcpy(tmp, b, s);
			_slice = _revid = new forestdb::revid(tmp, s);
		}
	}

	bufSize = 0;
	buffer = NULL;
}

void RevID::releaseData() {
	if (_revid != NULL) {
		if (_revid->buf != NULL) {
			::free((void*)_revid->buf);
			_revid->buf = NULL;
			_revid->size = 0;
		}
		delete _revid;
		_slice = _revid = NULL;
	}

	if(buffer != NULL){
		::free((void*)buffer);
		buffer = NULL;
	}
}

/**
 * RevIDBuffer - implementation
 */

RevIDBuffer::RevIDBuffer() {
	_slice = _revid = _revidBuffer = new forestdb::revidBuffer();
}

RevIDBuffer::RevIDBuffer(Slice& s) {
	_slice = _revid = _revidBuffer = new forestdb::revidBuffer(*s._slice);
}

RevIDBuffer::RevIDBuffer(const forestdb::revidBuffer& other) {
	_slice = _revid = _revidBuffer = new forestdb::revidBuffer(other);
}

RevIDBuffer::~RevIDBuffer() {
	if (_revidBuffer != NULL) {
		delete _revidBuffer;
		_slice = _revid =_revidBuffer = NULL;
	}
}

// Parses a regular (uncompressed) revID and compresses it.
// Throws BadRevisionID if the revID isn't in the proper format.
void RevIDBuffer::parse(Slice& s) {
	_revidBuffer->parse(*s._slice);
}

}
