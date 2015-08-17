/*
 * cbf_keystore.cc
 *
 *  Created on: Nov 19, 2014
 *      Author: hideki
 */

#include "KeyStore.hh"
#include "cbf_keystore.h"
#include "cbf_database.h"

#include <android/log.h>
 
/**
 * KeyStore - implementation
 */
namespace CBF {

KeyStore::KeyStore(){
	__android_log_write(ANDROID_LOG_WARN, "KeyStore::KeyStore()","start");
	_keyStore = NULL;
}

KeyStore::KeyStore(Database& db, std::string name){
	__android_log_write(ANDROID_LOG_WARN, "KeyStore::KeyStore(Database&, std::string)","start");
	_keyStore = new forestdb::KeyStore(db._db, name);
	__android_log_write(ANDROID_LOG_WARN, "KeyStore::KeyStore(Database&, std::string)","end");
}

KeyStore::~KeyStore()
{
	if (_keyStore != NULL) {
		delete _keyStore;
		_keyStore = NULL;
	}
}

KvsInfo KeyStore::getKvsInfo() const {
	return getKeyStore()->getInfo();
}

Sequence KeyStore::getLastSequence() const{
	return getKeyStore()->lastSequence();
}

std::string KeyStore::getName() const{
	return getKeyStore()->name();
}

Document* KeyStore::get(Slice& key, ContentOptions option) const {
	forestdb::Document fdoc = getKeyStore()->get(*key._slice,
			(forestdb::KeyStore::contentOptions) option);
	return new Document(fdoc);
}

Document* KeyStore::get(Sequence seq, ContentOptions option) const {
	forestdb::Document fdoc = getKeyStore()->get(seq,
			(forestdb::KeyStore::contentOptions) option);
	return new Document(fdoc);
}

// key must already be set

bool KeyStore::read(Document& doc, ContentOptions option) const {
	return getKeyStore()->read(*doc._doc,
			(forestdb::KeyStore::contentOptions) option);
}


Document* KeyStore::getByOffset(uint64_t offset, Sequence seq) {
	forestdb::Document fdoc = getKeyStore()->getByOffset(offset, seq);
	return new Document(fdoc);
}
void KeyStore::deleteKeyStore(Transaction& t) {
	getKeyStore()->deleteKeyStore(*t._trans);
}

void KeyStore::erase(Transaction& t) {
	getKeyStore()->erase(*t._trans);
}


/**
 * KeyStoreWriter - implementation
 */

KeyStoreWriter::KeyStoreWriter(KeyStore& s, forestdb::Transaction& t){
	_keyStoreWriter = new forestdb::KeyStoreWriter(*s.getKeyStore(), t);
}

Sequence KeyStoreWriter::set(Slice& key, Slice& meta, Slice& value) {
	return getKeyStoreWriter()->set(*key._slice, *meta._slice, *value._slice);
}

Sequence KeyStoreWriter::set(Slice& key, Slice& value) {
	return getKeyStoreWriter()->set(*key._slice, *value._slice);
}

void KeyStoreWriter::write(Document& doc) {
	getKeyStoreWriter()->write(*doc._doc);
}

bool KeyStoreWriter::del(Slice& key) {
	return getKeyStoreWriter()->del(*key._slice);
}

bool KeyStoreWriter::del(Sequence seq) {
	return getKeyStoreWriter()->del(seq);
}

bool KeyStoreWriter::del(Document& doc) {
	return getKeyStoreWriter()->del(*doc._doc);
}

void KeyStoreWriter::rollbackTo(Sequence seq) {
	getKeyStoreWriter()->rollbackTo(seq);
}

}
