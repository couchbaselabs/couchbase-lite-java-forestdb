/*
 * cbf_slice.h
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */
#ifndef CBF_SLICE_H_
#define CBF_SLICE_H_

#include <stdlib.h>
#include <string>
#include "slice.hh"

namespace CBF {

/**
 * Slice - wrapper class of forestdb::slice
 */
class Slice {

// member variables

private:
	forestdb::slice* _slice;

	friend class DocEnumerator;
	friend class Document;
	friend class KeyStore;
	friend class KeyStoreWriter;
	friend class Database;
	friend class Transaction;
	friend class RevID;
	friend class RevIDBuffer;
	friend class RevTree;
	friend class VersionedDocument;
	friend class Collatable;
	friend class CollatableReader;
	friend class IndexEnumerator;
	friend class KeyRange;
	friend class Index;
	friend class IndexWriter;

// member methods
public:

	Slice();
	Slice(const char* b, size_t s);
	// NOTE: SWIG can not recognize difference between std::string & char*
	//Slice(const char*);
	Slice(const forestdb::slice&) ;
	~Slice();

	// Return a pointer to the beginning of the referenced data
	char* getBuf();
	int compare(Slice&) ;
	Slice* copy();
	void free();

	// Return the length (in bytes) of the referenced data
	const int getSize();
	const void* getData();

private:
	// keep own memory because we don't know when original data is released.
	void init(const char*, size_t);
	void releaseData();
};

}

#endif // CBF_SLICE_H_
