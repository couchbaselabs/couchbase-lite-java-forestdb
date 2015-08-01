/**
 * cbf_mapreduceindex.h
 *
 *  Created on: Nov 15, 2014
 *      Author: hideki
 */

#ifndef CBF_MAPREDUCEINDEX_H_
#define CBF_MAPREDUCEINDEX_H_

#include "MapReduceIndex.hh"

#include "cbf_forestdb.h"
#include "cbf_database.h"
#include "cbf_collatable.h"
#include "cbf_index.h"

namespace CBF {

/**
 * Mappable - wrapper class of forestdb::Mappable
 */
class Mappable: public forestdb::Mappable {
public:
	Mappable(const Document& doc) :
			forestdb::Mappable(*doc._doc) {
	}
	virtual ~Mappable() {
	}
};

/**
 * EmitFn - wrapper class of forestdb::EmitFn
 */
class EmitFn {
private:
	forestdb::EmitFn* _emitFn;

public:
	EmitFn(forestdb::EmitFn* emit) :
			_emitFn(emit) {
	}
	virtual ~EmitFn() {
	}
	void call(Collatable& key, Collatable& value) {
		if (_emitFn != NULL) {
			(*_emitFn)(*key._collatable, *value._collatable);
		}
	}
};

/**
 * MapFn - wrapper class of forestdb::MapFn
 */
class MapFn: public forestdb::MapFn {
public:
	virtual ~MapFn() {
	}
	virtual void call(const Mappable&, EmitFn& emit) {
	}
	virtual void operator()(const forestdb::Mappable&, forestdb::EmitFn& emit); // Should be ignored by SWIG
};

/**
 * MapReduceIndex - wrapper class of forestdb::MapReduceIndex
 */
class MapReduceIndex : public Index {
private:
	KeyStore* _sourceStore;
protected:
	forestdb::MapReduceIndex * _mrindex;
	friend class MapReduceIndexer;
	friend class IndexEnumerator;

public:
	MapReduceIndex(Database&, std::string, KeyStore&);
	~MapReduceIndex();
	KeyStore* sourceStore() const;
	void readState();
	int indexType();
	void setup(Transaction&, int, MapFn*, std::string);
	Sequence lastSequenceIndexed() const;
	Sequence lastSequenceChangedAt() const;
	uint64_t rowCount() const;
	void erase(Transaction&);
};

// TODO: combination of composition & inheritance is complicated.
//       Need to think about simpler solution (just inheritance)
//       composition only does not work because of virtual function.
/**
 * BridgeMapReduceIndexer
 */
class BridgeMapReduceIndexer {
public:
	virtual ~BridgeMapReduceIndexer() {
	}
	virtual void bridgeAddDocument(const forestdb::Document& doc) = 0;
};

/**
 * MapReduceIndexer - wrapper class of forestdb::MapReduceIndexer
 */
class MapReduceIndexer : public BridgeMapReduceIndexer{

public:
	class InnerMapReduceIndexer: public forestdb::MapReduceIndexer {
	private:
		BridgeMapReduceIndexer* _bridge;
		friend class MapReduceIndexer;
	public:
		InnerMapReduceIndexer(BridgeMapReduceIndexer* bridge) :
			forestdb::MapReduceIndexer(), _bridge(bridge) {
		}
		virtual void addDocument(const forestdb::Document&);
		virtual void addMappable(const forestdb::Mappable&);
	};

private:
	InnerMapReduceIndexer* _mrindexer;

public:
    MapReduceIndexer();
    virtual ~MapReduceIndexer();
    void addIndex(MapReduceIndex*, Transaction&);
    void triggerOnIndex(MapReduceIndex*);
    bool run();
    virtual void addDocument(const Document*);
    virtual void bridgeAddDocument(const forestdb::Document&);
    void addMappable(const Mappable&);
};

}

#endif /* CBF_MAPREDUCEINDEX_H_ */
