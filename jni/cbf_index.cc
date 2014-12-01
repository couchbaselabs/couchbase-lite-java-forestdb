/*
 * cbf_index.cc
 *
 *  Created on: Nov 13, 2014
 *      Author: hideki
 */

#include "Index.hh"

#include "cbf_forestdb.h"
#include "cbf_slice.h"
#include "cbf_database.h"
#include "cbf_collatable.h"
#include "cbf_index.h"


namespace CBF {

/**
 * KeyRange - implementation
 */
KeyRange::KeyRange(Collatable& s, Collatable& e, bool inclusive)
{
	_range = new forestdb::KeyRange(*s._collatable, *e._collatable, inclusive);
}

KeyRange::KeyRange(Collatable& single){
	_range = new forestdb::KeyRange(*single._collatable);
}

KeyRange::KeyRange(const KeyRange& r){
	_range = new forestdb::KeyRange(*r._range);
}

bool KeyRange::isKeyPastEnd(Slice& key) const {
	return _range->isKeyPastEnd(*key._slice);
}

/**
 * Index - implementation
 */

Index::Index(forestdb::Index* index){
	_index = index;
}
Index::Index(Database& db, std::string name){
	_index = new forestdb::Index(db._db, name);
}

Index::~Index(){
	if (_index != NULL) {
		delete _index;
		_index = NULL;
	}
}

/**
 * IndexWriter - implementation
 */
IndexWriter::IndexWriter(Index& index, Transaction& t){
	_rowCount = 0;
	_writer = new forestdb::IndexWriter(index._index, *t._trans);
}

IndexWriter::~IndexWriter() {
	if (_writer != NULL) {
		delete _writer;
		_writer = NULL;
	}
}
bool IndexWriter::update(Slice& docID, Sequence seq, std::vector<Collatable> keys, std::vector<Collatable> values){
	std::vector<forestdb::Collatable> fkeys;
	std::vector<forestdb::Collatable> fvalues;
	for(std::vector<Collatable>::iterator it = keys.begin(); it != keys.end(); ++it){
		fkeys.push_back(*it->_collatable);
	}
	for(std::vector<Collatable>::iterator it = values.begin(); it != values.end(); ++it){
		fvalues.push_back(*it->_collatable);
	}
	return _writer->update(*docID._slice, seq, fkeys, fvalues, _rowCount);
}

uint64_t IndexWriter::getRowCount(){
	return _rowCount;
}

void IndexWriter::setRowCount(uint64_t rowCount){
	_rowCount = rowCount;
}

/**
 * IndexEnumerator - implementation
 */
IndexEnumerator::IndexEnumerator(Index& index, Collatable& startKey,
		Slice& startKeyDocID, Collatable& endKey, Slice& endKeyDocID,
		const DocEnumerator::Options options) {

	_enum = new forestdb::IndexEnumerator(index._index,
			*startKey._collatable, *startKeyDocID._slice, *endKey._collatable,
			*endKeyDocID._slice, options);
}

IndexEnumerator::IndexEnumerator(Index& index,
				std::vector<KeyRange*> keyRanges,
				const DocEnumerator::Options options,
				bool firstRead){

	std::vector<forestdb::KeyRange> _keyRanges;
	for (std::vector<KeyRange*>::iterator it = keyRanges.begin(); it != keyRanges.end(); ++it) {
		KeyRange* p = *it;
		_keyRanges.push_back(*p->_range);
	}
	_enum = new forestdb::IndexEnumerator(index._index, _keyRanges, options, firstRead);
}

CollatableReader* IndexEnumerator::key() const {
	return new CollatableReader(_enum->key());
}

CollatableReader* IndexEnumerator::value() const{
	return new CollatableReader(_enum->value());
}

Slice* IndexEnumerator::docID() const{
	return new Slice(_enum->docID());
}

Sequence IndexEnumerator::sequence() const{
	return _enum->sequence();
}

bool IndexEnumerator::next(){
	return _enum->next();
}

}



