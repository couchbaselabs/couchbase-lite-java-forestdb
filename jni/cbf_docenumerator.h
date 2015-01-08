/*
 * cbf_docenumerator.h
 *
 *  Created on: Nov 8, 2014
 *      Author: hideki
 */

#ifndef CBF_DOCENUMERATOR_H_
#define CBF_DOCENUMERATOR_H_

#include <string>

#include "DocEnumerator.hh"

#include "cbf_slice.h"
#include "cbf_database.h"


namespace CBF {

/**
 * DocEnumerator - wrapper class of forestdb::DocEnumerator
 */
class DocEnumerator {
public:
	struct Options: public forestdb::DocEnumerator::Options {
		static const Options Default;
		static Options getDef(){ return Default; }
		Options();
		~Options();
		ContentOptions getContentOption();
		void setContentOption(ContentOptions);
		bool isIncludeDeleted() const;
		void setIncludeDeleted(bool);
		bool isInclusiveEnd() const;
		void setInclusiveEnd(bool);
		bool isInclusiveStart() const;
		void setInclusiveStart(bool);
		unsigned getLimit() const;
		void setLimit(unsigned);
		unsigned getSkip() const;
		void setSkip(unsigned);
		bool isDescending() const;
		void setDescending(bool);
	};

private:
	friend class Slice;
	friend class Database;
	DocEnumerator(const DocEnumerator&); // no copying allowed
	forestdb::DocEnumerator* _enum;

public:
	DocEnumerator();
	DocEnumerator(KeyStore& store, const Slice& startKey = Slice::Null,
			const Slice& endKey = Slice::Null, const Options options =
					Options::Default);
	DocEnumerator(KeyStore& store, std::vector<std::string>& docIDs,
			const Options options = Options::Default);
	DocEnumerator(KeyStore& store, Sequence start, Sequence end = UINT64_MAX,
			const Options options = Options::Default);
	~DocEnumerator();
	bool next();
	void seek(Slice& key);
	Document* doc();
	void close();

};

}

#endif /* CBF_DOCENUMERATOR_H_ */
