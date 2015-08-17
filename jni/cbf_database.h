/*
 * cbf_database.h
 *
 *  Created on: Nov 12, 2014
 *      Author: hideki
 */

#ifndef CBF_DATABASE_H__
#define CBF_DATABASE_H__

/*****************************************************************************
 * cdf_database.h -
 * 	a wrapper of CBForest/Database.hh for non-C/C++ Language binding
 *****************************************************************************/

#include <string>
#include "Database.hh"	
#include "cbf_forestdb.h"
#include "cbf_keystore.h"

namespace CBF {

/**
 * Database - wrapper class of forestdb::Database
 * 		forestdb::Database
 * 			forestdb::KeyStore
 */
class Database : public KeyStore {
private:
	friend class KeyStore;
	friend class Transaction;
	friend class DocEnumerator;
	friend class VersionedDocument;
	friend class MapReduceIndex;
	friend class Index;

protected:
	forestdb::Database *_db;

	virtual forestdb::KeyStore* getKeyStore() const { return _db; }

public:
	static Config defaultConfig();

	// typedef fdb_open_flags openFlags;
	// typedef uint32_t fdb_open_flags;
	Database(std::string path, const Config& cfg);
	Database(forestdb::Database*);
	~Database();

	FileInfo getFileInfo() const;
	std::string getFilename() const;

	bool isReadOnly() const;

	void deleteDatabase();
	void erase();

	void compact();

	void commit();
};

/**
 * Transaction - wrapper class of forestdb::Transaction
 * 		forestdb::Transaction
 * 			forestdb::KeyStoreWriter
 * 				forestdb::KeyStore
 */
class Transaction : public KeyStoreWriter {
public:
	enum State {
		kNoOp, kAbort, kCommit
	};
private:
	forestdb::Transaction* _trans;
	friend class KeyStore;
	friend class VersionedDocument;
	friend class DocEnumerator;
	friend class IndexWriter;
	friend class MapReduceIndex;
	friend class MapReduceIndexer;

protected:
	virtual forestdb::KeyStoreWriter* getKeyStoreWriter() const { return _trans; }
	virtual forestdb::KeyStore*       getKeyStore()       const { return _trans; }

public:
	Transaction(Database* db);
	virtual ~Transaction();


	KeyStoreWriter* toKeyStoreWriter(KeyStore& s){ return new KeyStoreWriter(s, *_trans); }

	Database *getDatabase();
	State state() const;
	/** Tells the Transaction that it should rollback, not commit, when exiting scope. */
	void abort();

};
}

#endif // CBF_DATABASE_H__
