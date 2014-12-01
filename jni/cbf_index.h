/*
 * cbf_index.h
 *
 *  Created on: Nov 13, 2014
 *      Author: hideki
 */

#ifndef CBF_INDEX_H_
#define CBF_INDEX_H_

#include "Index.hh"

#include "cbf_forestdb.h"
#include "cbf_slice.h"
#include "cbf_database.h"
#include "cbf_docenumerator.h"
#include "cbf_collatable.h"
#include "cbf_keystore.h"

namespace CBF {

/**
 * KeyRange - wrapper class of forestdb::KeyRange
 */
class KeyRange {
private:
	forestdb::KeyRange* _range;
	friend class IndexEnumerator;
public:
	KeyRange(Collatable& s, Collatable& e, bool inclusive = true);
	KeyRange(Collatable&);
	KeyRange(const KeyRange &);
	bool isKeyPastEnd(Slice&) const;
};


/**
 * Index - wrapper class of forestdb::Index
 */
class Index {
// not inherit KeyStore because forestdb::KeyStore is protected for forestdb::Index

private:
protected:
	forestdb::Index * _index;
	friend class IndexWriter;
	friend class IndexEnumerator;

protected:
	Index(forestdb::Index* index);
	Index(){}


public:
	Index(Database&, std::string);
	~Index();
};

/**
 * IndexWriter - wrapper class of forestdb::IndexWriter
 */
class IndexWriter{
	// not inherit KeyStoreWriter because forestdb::KeyStoreWriter is protected for forestdb::IndexWriter
private:

	forestdb::IndexWriter * _writer;
	uint64_t _rowCount;


public:
	IndexWriter(Index& index, Transaction& t);
	~IndexWriter();
	bool update(Slice&, Sequence, std::vector<Collatable>, std::vector<Collatable>);
	uint64_t getRowCount();
	void setRowCount(uint64_t);
};


/**
 * IndexEnumerator - wrapper class of forestdb::IndexEnumerator
 */
class IndexEnumerator{

private:
	forestdb::IndexEnumerator* _enum;

public:
	IndexEnumerator(Index&,
					Collatable&,
					Slice&,
					Collatable&,
					Slice&,
					const DocEnumerator::Options options = DocEnumerator::Options::Default);
	IndexEnumerator(Index&,
					std::vector<KeyRange*> keyRanges,
					const DocEnumerator::Options options = DocEnumerator::Options::Default,
					bool firstRead =true);
	CollatableReader* key() const;
	CollatableReader* value() const;
	Slice* docID() const;
	Sequence sequence() const;
	bool next();
};

}

#endif /* CBF_INDEX_H_ */
