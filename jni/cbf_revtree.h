/*
 * cbf_revtree.h
 *
 *  Created on: Nov 11, 2014
 *      Author: hideki
 */

#ifndef CBF_REVTREE_H_
#define CBF_REVTREE_H_

#include "RevTree.hh"

#include "cbf_forestdb.h"
#include "cbf_revid.h"

namespace CBF {

class RevTree;

/**
 * Revision - wrapper class of forestdb::Revision
 */
class Revision {

private:
	forestdb::Revision* _revision;

	friend class RevTree;
	friend class VersionedDocument;

public:

	Revision();
	Revision(const forestdb::Revision*);

	RevTree* getOwner();
	RevID* getRevID();
	Sequence getSequence();

	Slice* getBody();
	bool isBodyAvailable() const;
	Slice* readBody() const;

	bool isLeaf() const;
	bool isDeleted() const;
	bool hasAttachments() const;
	bool isNew() const;
	bool isActive() const;

	unsigned index() const;
	const Revision* getParent() const;
	std::vector<Revision*> history() const;

	bool isSameAddress(Revision*); //for test
};


/**
 * RevTree - wrapper class of forestdb::RevTree
 *
 * NOTE: CouchBase Lite does not call RevTree directly. Only through VersionedDocument
 */
class RevTree {
private:
	int _httpStatus;
	forestdb::RevTree* _revtree;
	friend class VersionedDocument;
public:
	RevTree();
	RevTree(Slice&, Sequence, uint64_t);
	RevTree(const forestdb::RevTree*);
	~RevTree();

	void decode(Slice&, Sequence, uint64_t);
	Slice* encode();

	size_t size() const;
	const Revision* get(unsigned index) const;
	const Revision* get(RevID& revid) const;
	const Revision* getBySequence(Sequence seq) const;

	std::vector<Revision*> allRevisions() const;
	const Revision* currentRevision();
	std::vector<Revision*> currentRevisions() const;
	bool hasConflict() const;

	// httpStatus -> please use getLatestHttpStatus() method
	const Revision* insert(RevID&, Slice&, bool, bool, RevID&, bool);
	const Revision* insert(RevID&, Slice&, bool, bool, Revision*, bool);

	const int getLatestHttpStatus();

	unsigned prune(unsigned);

	int purge(RevID&);

	void sort();

};

}

#endif /* CBF_REVTREE_H_ */
