/*
 * cbf_versioneddocument.cc
 *
 *  Created on: Nov 19, 2014
 *      Author: hideki
 */

#include "cbf_versioneddocument.h"

namespace CBF {

/**
 * VersionedDocument - implementation
 */
VersionedDocument::VersionedDocument(KeyStore& store, Slice& docID) {
	_revtree = _vdoc = new forestdb::VersionedDocument(*store.getKeyStore(),
			*docID._slice);
	_httpStatus = 0;
}

VersionedDocument::VersionedDocument(KeyStore& store, const Document& doc) {
	_revtree = _vdoc = new forestdb::VersionedDocument(*store.getKeyStore(),
			*doc._doc);
	_httpStatus = 0;
}

VersionedDocument::~VersionedDocument() {
	if (_vdoc != NULL) {
		delete _vdoc;
		_revtree = _vdoc = NULL;
	}
}

void VersionedDocument::read() {
	_vdoc->read();
}

bool VersionedDocument::revsAvailable() const {
	return _vdoc->revsAvailable();
}

Slice* VersionedDocument::getDocID() const {
	return new Slice(_vdoc->docID());
}

// revID
RevID* VersionedDocument::getRevID() const {
	return new RevID(_vdoc->revID());
}
// flags
VersionedDocument::Flags VersionedDocument::getFlags() const {
	return _vdoc->flags();
}

bool VersionedDocument::isDeleted() const {
	return _vdoc->isDeleted();
}
bool VersionedDocument::isConflicted() const {
	return _vdoc->isConflicted();
}
bool VersionedDocument::hasAttachments() const {
	return _vdoc->hasAttachments();
}

bool VersionedDocument::exists() const {
	return _vdoc->exists();
}

Sequence VersionedDocument::getSequence() const {
	return _vdoc->sequence();
}

bool VersionedDocument::changed() const {
	return _vdoc->changed();
}

void VersionedDocument::save(Transaction& transaction) {
	_vdoc->save(*transaction._trans);
}

}

