/*
 * cbf_mapreduce.cc
 *
 *  Created on: Nov 15, 2014
 *      Author: hideki
 */

#include "MapReduceIndex.hh"

#include "cbf_keystore.h"
#include "cbf_mapreduceindex.h"

#include <android/log.h>

namespace CBF {

/**
 * MapFn - implementation
 */
void MapFn::operator()(const forestdb::Mappable& mappable,
		forestdb::EmitFn& emit) {
	EmitFn _emit(&emit);
	this->call((const Mappable&) mappable, _emit);
}

/**
 * MapReduceIndex - implementation
 */
MapReduceIndex::MapReduceIndex(Database& db, std::string name, KeyStore& sourceStore){
	_mrindex = new forestdb::MapReduceIndex(db._db, name, *sourceStore.getKeyStore());
	_index = _mrindex; // because of combination of composition and inheritance...

	_sourceStore = &sourceStore;
}

MapReduceIndex::~MapReduceIndex(){
	if(_mrindex!=NULL){
		delete _mrindex;
		_mrindex = NULL;
	}
	_index = NULL;
}
KeyStore* MapReduceIndex::sourceStore() const { 
	return _sourceStore; 
}
void MapReduceIndex::readState() {
	_mrindex->readState();
}

int MapReduceIndex::indexType() {
	return _mrindex->indexType();
}

void MapReduceIndex::setup(Transaction& t, int indexType, MapFn *map, std::string mapVersion) {
	_mrindex->setup(*t._trans, indexType, (forestdb::MapFn*) map, mapVersion);
}

Sequence MapReduceIndex::lastSequenceIndexed() const {
	return _mrindex->lastSequenceIndexed();
}

Sequence MapReduceIndex::lastSequenceChangedAt() const {
	return _mrindex->lastSequenceChangedAt();
}

uint64_t MapReduceIndex::rowCount() const {
	return _mrindex->rowCount();
}
void MapReduceIndex::erase(Transaction& t) {
	_mrindex->erase(*t._trans);
}

/**
 * MapReduceIndexer - implementation
 * 		NOTE: composition
 */
void MapReduceIndexer::InnerMapReduceIndexer::addDocument(
		const forestdb::Document& doc) {
	_bridge->bridgeAddDocument(doc);
}

void MapReduceIndexer::InnerMapReduceIndexer::addMappable(
		const forestdb::Mappable& mappable) {
	__android_log_write(ANDROID_LOG_WARN, "CBF::MapReduceIndexer::InnerMapReduceIndexer::addMappable()","start");
	try{
		forestdb::MapReduceIndexer::addMappable(mappable);
	}
	catch(forestdb::error x){
		char buff[1024];
		sprintf(buff, "Error indexing: ForestDB error %d", x.status);
		__android_log_write(ANDROID_LOG_WARN, "CBF::MapReduceIndexer::InnerMapReduceIndexer::addMappable()",buff);
	}
	catch(...){
		__android_log_write(ANDROID_LOG_WARN, "CBF::MapReduceIndexer::InnerMapReduceIndexer::addMappable()","Unexpected exception indexing");
	}

	__android_log_write(ANDROID_LOG_WARN, "CBF::MapReduceIndexer::InnerMapReduceIndexer::addMappable()","end");
}

MapReduceIndexer::MapReduceIndexer() {
	_mrindexer = new InnerMapReduceIndexer(this);
}

MapReduceIndexer::~MapReduceIndexer() {
	if (_mrindexer != NULL) {
		delete _mrindexer;
		_mrindexer = NULL;
	}
}
void MapReduceIndexer::addIndex(MapReduceIndex* index, Transaction& t){
	_mrindexer->addIndex(index->_mrindex, t._trans);
}
void MapReduceIndexer::triggerOnIndex(MapReduceIndex* index) {
	_mrindexer->triggerOnIndex(index->_mrindex);
}

bool MapReduceIndexer::run() {
	return _mrindexer->run();
}

void MapReduceIndexer::addDocument(const Document* doc) {
	_mrindexer->addDocument(*doc->_doc);
}

void MapReduceIndexer::bridgeAddDocument(const forestdb::Document& doc) {
	addDocument(new Document(doc));
}

void MapReduceIndexer::addMappable(const Mappable& mappable) {
	__android_log_write(ANDROID_LOG_WARN, "CBF::MapReduceIndexer::addMappable()","start");
	_mrindexer->addMappable(mappable);
	__android_log_write(ANDROID_LOG_WARN, "CBF::MapReduceIndexer::addMappable()","end");
}

}



