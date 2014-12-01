/*
 * cbf_collatable.h
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */

#ifndef CBF_COLLATABLE_H_
#define CBF_COLLATABLE_H_

#include "Collatable.hh"

namespace CBF {

class Slice;

/**
 * Collatable - wrapper class of forestdb::Collatable
 */
class Collatable {
private:
	forestdb::Collatable* _collatable;
	friend class KeyRange;
	friend class IndexWriter;
	friend class IndexEnumerator;
	friend class EmitFn;

public:
	Collatable();

	Collatable(const forestdb::Collatable& v);

	Collatable(const bool v);
	Collatable(const double v);
	Collatable(const char* v);
	Collatable(const Collatable& v);
	Collatable(const Slice& v);

	~Collatable();

	Collatable* addNull();
	Collatable* add(const bool);
	Collatable* add(const double);
	Collatable* add(const Collatable&);
	Collatable* add(const char*);
	Collatable* add(const Slice&);

	Collatable* beginArray();
	Collatable* endArray();

	Collatable* beginMap();
	Collatable* endMap();

	Collatable* addSpecial();

	Slice* toSlice() const; // operator slice() const
	size_t size() const;
	bool empty() const;

	std::string dump();
};

/**
 * CollatableReader - wrapper class of forestdb::CollatableReader
 */
class CollatableReader {
private:
	forestdb::CollatableReader* _reader;
	Slice* _slice;
	friend class IndexEnumerator;

public:
	CollatableReader(const Slice&);
	CollatableReader(const forestdb::CollatableReader&);
	~CollatableReader();
	Slice* data() const;
	int peekTag();
	int64_t readInt();
	double readDouble();
	Slice* readString();
	Slice* read();

	void beginArray();
	void endArray();
	void beginMap();
	void endMap();

	std::string dump();
};

}

#endif /* CBF_COLLATABLE_H_ */
