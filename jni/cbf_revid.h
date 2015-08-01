/*
 * cbf_revid.h
 *
 *  Created on: Nov 11, 2014
 *      Author: hideki
 */

#ifndef CBF_REVID_H_
#define CBF_REVID_H_

#include "RevID.hh"

#include "cbf_slice.h"

namespace CBF {

class RevID;
class RevIDBuffer;

/**
 * RevID - wrapper class of forestdb::RevID
 */
class RevID : public Slice{

private:
	forestdb::revid* _revid;
	size_t bufSize;
	friend class RevTree;
	friend class VersionedDocument;
	friend class RevIDBuffer;

public:
	RevID();
	RevID(const char*, size_t);
	RevID(const char*);
	RevID(const forestdb::revid&);
	RevID(const forestdb::slice&);
	~RevID();

	bool isCompressed() const;
	Slice *expanded() const;
	size_t expandedSize() const;
	bool expandInto(Slice &dst) const;
	unsigned generation() const;
	Slice* digest() const;

	// overwrite Slice::getBuf()
	char* getBuf();
	char* toString(){ return getBuf(); }

	size_t getBufSize(){ return bufSize; }
private:
	void init(const char*, size_t);
	void releaseData();
};

/**
 * RevIDBuffer - wrapper class of forestdb::RevIDBuffer
 */
class RevIDBuffer : public RevID {

private:
	forestdb::revidBuffer* _revidBuffer;

	friend class RevID;
	friend class RevTree;
	friend class VersionedDocument;

public:
	RevIDBuffer();
	RevIDBuffer(Slice&);
	RevIDBuffer(const forestdb::revidBuffer&);
	~RevIDBuffer();

	void parse(Slice&);
};

}

#endif /* CBF_REVID_H_ */
