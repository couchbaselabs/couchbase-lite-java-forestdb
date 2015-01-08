/*
 * cbf_docenumerator.cc
 *
 *  Created on: Nov 18, 2014
 *      Author: hideki
 */

#include "cbf_docenumerator.h"

namespace CBF {

/**
 * DocEnumerator::Options
 */
const DocEnumerator::Options DocEnumerator::Options::Default;

DocEnumerator::Options::Options() {
	skip = 0;
	limit = UINT_MAX;
	descending = false;
	inclusiveStart = true;
	inclusiveEnd = true;
	includeDeleted = false;
	contentOptions = forestdb::Database::kDefaultContent;
}

DocEnumerator::Options::~Options(){
}

ContentOptions DocEnumerator::Options::getContentOption() {
	return (ContentOptions) this->contentOptions;
}

void DocEnumerator::Options::setContentOption(ContentOptions contentOptions) {
	this->contentOptions = (forestdb::Database::contentOptions) contentOptions;
}

bool DocEnumerator::Options::isIncludeDeleted() const {
	return includeDeleted;
}

void DocEnumerator::Options::setIncludeDeleted(bool includeDeleted) {
	this->includeDeleted = includeDeleted;
}

bool DocEnumerator::Options::isInclusiveEnd() const {
	return inclusiveEnd;
}

void DocEnumerator::Options::setInclusiveEnd(bool inclusiveEnd) {
	this->inclusiveEnd = inclusiveEnd;
}
bool DocEnumerator::Options::isInclusiveStart() const {
	return inclusiveStart;
}

void DocEnumerator::Options::setInclusiveStart(bool inclusiveStart) {
	this->inclusiveStart = inclusiveStart;
}

unsigned DocEnumerator::Options::getLimit() const {
	return limit;
}

void DocEnumerator::Options::setLimit(unsigned limit) {
	this->limit = limit;
}

unsigned DocEnumerator::Options::getSkip() const {
	return skip;
}

void DocEnumerator::Options::setSkip(unsigned skip) {
	this->skip = skip;
}
bool DocEnumerator::Options::isDescending() const {
	return descending;
}
void DocEnumerator::Options::setDescending(bool descending){
	this->descending = descending;
}
/**
 * DocEnumerator
 */
DocEnumerator::DocEnumerator() {
	_enum = new forestdb::DocEnumerator();
}

DocEnumerator::DocEnumerator(KeyStore& store, const Slice& startKey, const Slice& endKey, const Options options) {
	_enum = new forestdb::DocEnumerator(*store.getKeyStore(), *startKey._slice, *endKey._slice, options);
}

DocEnumerator::DocEnumerator(KeyStore& store, std::vector<std::string>& docIDs, const Options options) {
	_enum = new forestdb::DocEnumerator(*store.getKeyStore(), docIDs, options);
}

DocEnumerator::DocEnumerator(KeyStore& store, Sequence start, Sequence end, const Options options) {
	_enum = new forestdb::DocEnumerator(*store.getKeyStore(), start, end, options);
}


DocEnumerator::~DocEnumerator() {
	if (_enum != NULL) {
		delete _enum;
		_enum = NULL;
	}
}

bool DocEnumerator::next() {
	return _enum->next();
}

void DocEnumerator::seek(Slice& key) {
	_enum->seek(*key._slice);
}

Document* DocEnumerator::doc() {
	return new Document(_enum->doc());
}

void DocEnumerator::close() {
	return _enum->close();
}

}
