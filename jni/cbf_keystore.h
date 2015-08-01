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
protected:
    virtual forestdb::KeyStore* getKeyStore() const = 0;
    KeyStore(){} // no allow to create KeyStore instance

public:
    virtual ~KeyStore(){}

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
	virtual forestdb::KeyStoreWriter* getKeyStoreWriter() const = 0;

	KeyStoreWriter(){} // no allow to create KeyStoreWriter instance

public:
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
