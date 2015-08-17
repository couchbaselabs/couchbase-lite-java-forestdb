/*
 * cbf_keystore.h
 *
 *  Created on: Nov 19, 2014
 *      Author: hideki
 */

#ifndef CBF_KEYSTORE_H_
#define CBF_KEYSTORE_H_

#include "KeyStore.hh"

#include "cbf_slice.h"
#include "cbf_document.h"

namespace CBF {
	class Database;
    class Document;
    class KeyStoreWriter;
    class Transaction;

/**
 * KeyStore - wrapper class of forestdb::KeyStore
 */
class KeyStore {
private:
	friend class DocEnumerator;
	friend class MapReduceIndex;
	friend class VersionedDocument;
	friend class KeyStoreWriter;

protected:
	forestdb::KeyStore * _keyStore;

	// SWIG won't generate wrappers for a class if it appears to be abstract--that is,
	// it has undefined pure virtual methods.
    //virtual forestdb::KeyStore* getKeyStore() const = 0;
    virtual forestdb::KeyStore* getKeyStore() const { return _keyStore; }
    
    KeyStore();
public:
	
	KeyStore(Database& db, std::string name);
    virtual ~KeyStore();


	KvsInfo getKvsInfo() const;
	Sequence getLastSequence() const;
	std::string getName() const;

	Document* get(Slice& key, ContentOptions option = kDefaultContent) const;
	Document* get(Sequence seq, ContentOptions option = kDefaultContent) const;
	bool read(Document& doc, ContentOptions option = kDefaultContent) const;

	Document* getByOffset(uint64_t, Sequence);

	void deleteKeyStore(Transaction&);
	void erase(Transaction&);
};

/**
 * KeyStoreWriter - wrapper class of forestdb::KeyStoreWriter
 */
class KeyStoreWriter : public KeyStore {

protected:
	forestdb::KeyStoreWriter * _keyStoreWriter;
	
	//virtual forestdb::KeyStoreWriter* getKeyStoreWriter() const = 0;
	virtual forestdb::KeyStoreWriter* getKeyStoreWriter() const { return _keyStoreWriter; }

	KeyStoreWriter(){ _keyStoreWriter = NULL; }
	
public:
	KeyStoreWriter(KeyStore&, forestdb::Transaction&);
	virtual ~KeyStoreWriter(){}

	Sequence set(Slice&, Slice&, Slice&);
	Sequence set(Slice&, Slice&);
	void write(Document&);

	bool del(Slice&);
	bool del(Sequence);
	bool del(Document&);

	void rollbackTo(Sequence);
};

}

#endif /* CBF_KEYSTORE_H_ */
