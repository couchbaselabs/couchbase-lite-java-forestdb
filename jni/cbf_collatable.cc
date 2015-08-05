/*
 * cbf_collatable.cc
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */
#include <android/log.h>

#include "slice.hh"
#include "Collatable.hh"

#include "cbf_slice.h"
#include "cbf_collatable.h"

namespace CBF {

/**
 * Collatable - implementation
 */
Collatable::Collatable() {
	_collatable = new forestdb::Collatable();
}

Collatable::Collatable(const forestdb::Collatable& v) {
	_collatable = new forestdb::Collatable();
	*_collatable << v;
}

Collatable::Collatable(const bool v) :
		Collatable() {
	add(v);
}
Collatable::Collatable(const double v) :
		Collatable() {
	add(v);
}
Collatable::Collatable(const char* v) :
		Collatable() {
	add(v);
}
Collatable::Collatable(const Collatable& v) :
		Collatable() {
	add(v);
}
Collatable::Collatable(const Slice& v) :
		Collatable() {
	add(v);
}

Collatable::~Collatable() {
	if(_collatable != NULL){
		//__android_log_write(ANDROID_LOG_WARN, "Collatable::~Collatable()","delete _collatable;");
		delete _collatable;
		_collatable = NULL;
	}
}

Collatable* Collatable::addNull() {
	_collatable->addNull();
	return this;

}

Collatable* Collatable::add(const bool v) {
	_collatable->addBool(v);
	return this;
}

Collatable* Collatable::add(const double v) {
	*_collatable << v;
	return this;
}

Collatable* Collatable::add(const Collatable& v) {
	*_collatable << *v._collatable;
	return this;
}

Collatable* Collatable::add(const char* v) {
	*_collatable << v;
	return this;
}

Collatable* Collatable::add(const Slice& v) {
	*_collatable << *v._slice;
	return this;
}

Collatable* Collatable::beginArray() {
	_collatable->beginArray();
	return this;
}

Collatable* Collatable::endArray() {
	_collatable->endArray();
	return this;
}

Collatable* Collatable::beginMap() {
	_collatable->beginMap();
	return this;
}

Collatable* Collatable::endMap() {
	_collatable->endMap();
	return this;
}

Collatable* Collatable::addSpecial() {
	_collatable->addSpecial();
	return this;
}
Slice*Collatable::toSlice() const {
	return new Slice(forestdb::slice(*_collatable));
}
size_t Collatable::size() const {
	return _collatable->size();
}

bool Collatable::empty() const {
	return _collatable->empty();
}

std::string Collatable::dump() {
	return _collatable->dump();
}

/**
 * CollatableReader
 */
CollatableReader::CollatableReader(const forestdb::CollatableReader& reader) {
	_slice = new Slice(reader.data());
	_reader = new forestdb::CollatableReader(*_slice->_slice);
}
CollatableReader::CollatableReader(const Slice& slice) {
	_slice = NULL;
	_reader = new forestdb::CollatableReader(*slice._slice);
}

CollatableReader::~CollatableReader() {
	if (_slice != NULL) {
		delete _slice;
		_slice = NULL;
	}
	if (_reader != NULL) {
		delete _reader;
		_reader = NULL;
	}
}
int CollatableReader::peekTag() {
	return (int) _reader->peekTag();
}
Slice* CollatableReader::data() const {
	return new Slice((const forestdb::slice&) _reader->data());
}

int64_t CollatableReader::readInt() {
	return _reader->readInt();
}
double CollatableReader::readDouble() {
	return _reader->readDouble();
}
Slice* CollatableReader::readString() {
	return new Slice(_reader->readString());
}
Slice* CollatableReader::read() {
	return new Slice(_reader->read());
}

void CollatableReader::beginArray() {
	_reader->beginArray();
}
void CollatableReader::endArray() {
	_reader->endArray();
}
void CollatableReader::beginMap() {
	_reader->beginMap();
}

void CollatableReader::endMap() {
	_reader->endMap();
}

std::string CollatableReader::dump() {
	return _reader->dump();
}

}
