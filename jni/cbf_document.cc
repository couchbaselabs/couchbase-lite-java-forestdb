/*
 * cbf_document.cc
 *
 *  Created on: Nov 19, 2014
 *      Author: hideki
 */


#include "cbf_document.h"

namespace CBF {

/**
 * Document
 */
Document::Document() {
	_doc = new forestdb::Document();
}

Document::Document(Slice& key) {
	_doc = new forestdb::Document(forestdb::slice(key.getBuf(), key.getSize()));
}

Document::Document(const forestdb::Document& doc) {
	_doc = new forestdb::Document(doc);
}

Document::~Document() {
	if (_doc != NULL) {
		delete _doc;
		_doc = NULL;
	}
}

Slice* Document::getKey() {
	return new Slice(_doc->key());
}

Slice* Document::getMeta() {
	return new Slice(_doc->meta());
}

Slice* Document::getBody() {
	return new Slice(_doc->body());
}

void Document::setKey(Slice& key) {
	_doc->setKey(*key._slice);
}

void Document::setMeta(Slice& meta) {
	_doc->setMeta(*meta._slice);
}

void Document::setBody(Slice& body) {
	_doc->setBody(*body._slice);
}

Slice* Document::resizeMeta(size_t newSize) {
	return new Slice(_doc->resizeMeta(newSize));
}

void Document::clearMetaAndBody() {
	_doc->clearMetaAndBody();
}

Sequence Document::getSequence() const {
	return _doc->sequence();
}

uint64_t Document::offset() const {
	return _doc->offset();
}

size_t Document::sizeOnDisk() const {
	return _doc->sizeOnDisk();
}

bool Document::deleted() const {
	return _doc->deleted();
}

bool Document::exists() const {
	return _doc->exists();
}

bool Document::valid() const {
	return _doc->valid();
}

void Document::updateSequence(Sequence s) {
	_doc->updateSequence(s);
}

}

