/*
 * cbf_document.h
 *
 *  Created on: Nov 19, 2014
 *      Author: hideki
 */

#ifndef CBF_DOCUMENT_H_
#define CBF_DOCUMENT_H_

#include "Document.hh"

#include "cbf_forestdb.h"
#include "cbf_slice.h"

namespace CBF {

class MapReduceIndex;

/**
 * Document - wrapper class of forestdb::Document
 */
class Document {

private:
	forestdb::Document* _doc;
	friend class KeyStore;
	friend class KeyStoreWriter;
	friend class Transaction;
	friend class Database;
	friend class VersionedDocument;
	friend class Mappable;
	friend class MapReduceIndex;
	friend class MapReduceIndexer;

public:
	Document();
	Document(Slice& key);
	Document(const forestdb::Document& doc);
	~Document();

	Slice* getKey();
	Slice* getMeta();
	Slice* getBody();
	void setKey(Slice& key);
	void setMeta(Slice& meta);
	void setBody(Slice& body);

	Slice* resizeMeta(size_t newSize);
	void clearMetaAndBody();
	Sequence getSequence() const;
	uint64_t offset() const;
	size_t sizeOnDisk() const;
	bool deleted() const;
	bool exists() const;
	bool valid() const;
	void updateSequence(Sequence s);
};


}


#endif /* CBF_DOCUMENT_H_ */
