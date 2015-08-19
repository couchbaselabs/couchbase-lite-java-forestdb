/*
 * cbf_revtree.cc
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */

#include "cbf_revtree.h"
//#include <android/log.h>

namespace CBF {

/**
 * Revision - implementation
 */
Revision::Revision() {
	_revision = new forestdb::Revision();
}

Revision::Revision(const forestdb::Revision* revision) {
	_revision = (forestdb::Revision*) revision;
}

RevTree* Revision::getOwner() {
	return new RevTree(_revision->owner);
}

RevID* Revision::getRevID() {
	return new RevID(_revision->revID);
}

Sequence Revision::getSequence() {
	return _revision->sequence;
}

bool Revision::isBodyAvailable() const {
	return _revision->isBodyAvailable();
}

Slice* Revision::readBody() const {
	return new Slice(_revision->readBody());
}

bool Revision::isLeaf() const {
	return _revision->isLeaf();
}

bool Revision::isDeleted() const {
	return _revision->isDeleted();
}

bool Revision::hasAttachments() const {
	return _revision->hasAttachments();
}

bool Revision::isNew() const {
	return _revision->isNew();
}

bool Revision::isActive() const {
	return _revision->isActive();
}

unsigned Revision::index() const {
	return _revision->index();
}

const Revision* Revision::getParent() const {
	const forestdb::Revision* rev = _revision->parent();
	return rev == NULL ? NULL : new Revision(rev);
}

std::vector<Revision*> Revision::history() const {
	std::vector<const forestdb::Revision*> revs = _revision->history();
	std::vector<Revision*> results;
	for (std::vector<const forestdb::Revision*>::iterator it = revs.begin();
			it != revs.end(); ++it) {
		results.push_back(new CBF::Revision(*it));
	}
	return results;
}

bool Revision::isSameAddress(Revision* other) {
	return this->_revision == other->_revision;
}

/**
 * RevTree - implementation
 */
RevTree::RevTree() {
	_revtree = new forestdb::RevTree();
	_httpStatus = 0;
}
RevTree::RevTree(Slice& raw_tree, Sequence seq, uint64_t docOffset) {
	_revtree = new forestdb::RevTree(*raw_tree._slice, seq, docOffset);
	_httpStatus = 0;
}

// TODO: Not sure if need to copy RevTree.
RevTree::RevTree(const forestdb::RevTree* tree) {
	_revtree = (forestdb::RevTree*) tree;
	_httpStatus = 0;
}

RevTree::~RevTree() {
	if (_revtree != NULL) {
		delete _revtree;
		_revtree = NULL;
	}
}

void RevTree::decode(Slice& raw_tree, Sequence seq, uint64_t docOffset) {
	_revtree->decode((forestdb::slice) *raw_tree._slice,
			(forestdb::sequence) seq, docOffset);
}

Slice* RevTree::encode() {
	return new Slice(_revtree->encode());
}

size_t RevTree::size() const {
	return _revtree->size();
}
const Revision* RevTree::get(unsigned index) const {
	const forestdb::Revision* rev = _revtree->get(index);
	return rev == NULL ? NULL : new Revision(rev);
}
const Revision* RevTree::get(RevID& revid) const {
	const forestdb::Revision* rev = _revtree->get(*revid._revid);
	return rev == NULL ? NULL : new Revision(rev);
}

const Revision* RevTree::getBySequence(Sequence seq) const {
	const forestdb::Revision* rev = _revtree->getBySequence(seq);
	return rev == NULL ? NULL : new Revision(rev);
}

std::vector<Revision*> RevTree::allRevisions() const {
	const std::vector<forestdb::Revision>& revs = _revtree->allRevisions();
	std::vector<Revision*> results;
	for (int i = 0; i < revs.size(); i++) {
		results.push_back(new Revision(&revs[i]));
	}
	return results;
}

const Revision* RevTree::currentRevision() {
	const forestdb::Revision* rev = _revtree->currentRevision();
	return rev == NULL ? NULL : new Revision(rev);
}

std::vector<Revision*> RevTree::currentRevisions() const {
	std::vector<const forestdb::Revision*> revs = _revtree->currentRevisions();
	std::vector<Revision*> results;
	for (std::vector<const forestdb::Revision*>::iterator it = revs.begin();
			it != revs.end(); ++it) {
		results.push_back(new CBF::Revision(*it));
	}
	return results;
}

bool RevTree::hasConflict() const {
	return _revtree->hasConflict();
}

const Revision* RevTree::insert(RevID& revID, Slice& body, bool deleted,
		bool hasAttachments, RevID& parentRevID, bool allowConflict) {
	const forestdb::Revision* rev = _revtree->insert(*revID._revid,
			*body._slice, deleted, hasAttachments, *parentRevID._revid,
			allowConflict, _httpStatus);

	return rev == NULL ? NULL : new Revision(rev);
}

const Revision* RevTree::insert(RevID& revID, Slice& body, bool deleted,
		bool hasAttachments, Revision* parent, bool allowConflict) {
	const forestdb::Revision* rev = _revtree->insert(*revID._revid,
			*body._slice, deleted, hasAttachments,
			(parent == NULL) ? NULL : parent->_revision, allowConflict,
			_httpStatus);
	return rev == NULL ? NULL : new Revision(rev);
}
int RevTree::insertHistory(std::vector<RevID*> history, Slice& body, bool deleted, bool hasAttachments){
	std::vector<forestdb::revid> _history;
	for (std::vector<RevID*>::iterator it = history.begin(); it != history.end(); ++it) {
		RevID* p = *it;
		_history.push_back(*p->_revid);
	}
	return _revtree->insertHistory(_history, *body._slice, deleted, hasAttachments);
}
const int RevTree::getLatestHttpStatus() {
	return _httpStatus;
}

unsigned RevTree::prune(unsigned maxDepth) {
	return _revtree->prune(maxDepth);
}

int RevTree::purge(RevID& revID) {
	return _revtree->purge(*revID._revid);
}

void RevTree::sort() {
	_revtree->sort();
}

}
