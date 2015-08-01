/*
 * cbf_database.cc
 *
 *  Created on: Nov 18, 2014
 *      Author: hideki
 */

#include "cbf_database.h"

namespace CBF {

/**
 * Database - implementation
 */
Config Database::defaultConfig() {
	return Config(forestdb::Database::defaultConfig());
}

Database::Database(std::string path, const Config& cfg) {
	_db = new forestdb::Database(path, cfg);
}

Database::Database(forestdb::Database* database) {
	_db = database;
}

Database::~Database() {
	if (_db != NULL) {
		delete _db;
		_db = NULL;
	}
}

std::string Database::getFilename() const {
	return _db->filename();
}

FileInfo Database::getFileInfo() const {
	return _db->getInfo();
}

bool Database::isReadOnly() const {
	return _db->isReadOnly();
}

void Database::deleteDatabase() {
	_db->deleteDatabase();
}
void Database::erase() {
	_db->erase();
}

void Database::compact() {
	_db->compact();
}

void Database::commit() {
	_db->commit();
}

/**
 * Transaction - implementation
 */
Transaction::Transaction(Database* db) {
	_trans = new forestdb::Transaction(db->_db);
}

Transaction::~Transaction() {
	if (_trans != NULL) {
		delete _trans;
		_trans = NULL;
	}
}

Database* Transaction::getDatabase() {
	return new Database(_trans->database());
}

Transaction::State Transaction::state() const{
	return (Transaction::State)_trans->state();
}

void Transaction::abort() {
	_trans->abort();
}

}
