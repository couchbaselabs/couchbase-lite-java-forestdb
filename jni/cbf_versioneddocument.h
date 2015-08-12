/*
 * cbf_versioneddocument.h
 *
 *  Created on: Nov 11, 2014
 *      Author: hideki
 */

#ifndef CBF_VERSIONEDDOCUMENT_H_
#define CBF_VERSIONEDDOCUMENT_H_

#include "VersionedDocument.hh"

#include "cbf_forestdb.h"
#include "cbf_slice.h"
#include "cbf_database.h"
#include "cbf_revid.h"
#include "cbf_revtree.h"

namespace CBF {

/**
 * VersionedDocument - wrapper class of forestdb::VersionedDocument
 */
class VersionedDocument: public RevTree {
public:
	/** Flags that apply to the document as a whole */
	typedef uint8_t Flags;
	enum {
		kDeleted = 0x01,
		kConflicted = 0x02,
		kHasAttachments = 0x04
	};

private:
	int _httpStatus;
	forestdb::VersionedDocument* _vdoc;

public:
	VersionedDocument(KeyStore&, Slice&);
	VersionedDocument(KeyStore&, const Document&);
	~VersionedDocument();

	/** Reads and parses the body of the document. Useful if doc was read as meta-only. */
	void read();
	
	/** Returns false if the document was loaded metadata-only. Revision accessors will fail. */
	bool revsAvailable() const;

	Slice* getDocID() const;
	RevID* getRevID() const;
	Flags getFlags() const;
	bool isDeleted() const;
	bool isConflicted() const;
	bool hasAttachments() const;
	
	bool exists() const;
	Sequence getSequence() const;
	
	Slice* getDocType() const;
	void setDocType(Slice&);

	bool changed() const;
	void save(Transaction&);
};

}

#endif /* CBF_VERSIONEDDOCUMENT_H_ */
