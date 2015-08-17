/*
 * cbf_forestdb.h
 *
 *  Created on: Nov 10, 2014
 *      Author: hideki
 */

#ifndef CBF_FORESTDB_H_
#define CBF_FORESTDB_H_


#include "Database.hh"

namespace CBF {

typedef uint64_t fdb_seqnum_t;
typedef fdb_seqnum_t Sequence;  //typedef fdb_seqnum_t sequence;


// enum does not work well if enum is defined in class
enum ContentOptions {
	kDefaultContent = 0,
	kMetaOnly = 0x01
};

// from fdb_types.h
enum OpenFlags {
	FDB_OPEN_FLAG_CREATE = 1,
	FDB_OPEN_FLAG_RDONLY = 2
};

//typedef fdb_info info;
struct KvsInfo: public fdb_kvs_info {
	KvsInfo() { }
	KvsInfo(const fdb_kvs_info& info) : fdb_kvs_info(info) { }
	const char* getName(){ return name; }
	uint64_t getLastSeqnum(){ return last_seqnum; }
	uint64_t getDocCount(){ return doc_count; }
	uint64_t getSpaceUsed(){ return space_used; }
};

struct FileInfo: public fdb_file_info {
	FileInfo() { }
	FileInfo(const fdb_file_info& info) : fdb_file_info(info) { }
	const char* getFilename() { return filename; }
	const char* getNewFilename() { return new_filename; }
	uint64_t getDocCount() { return doc_count; }
	uint64_t getSpaceUsed() { return space_used; }
	uint64_t getFileSize() { return file_size; }
};

// TODO: some of forestdb custom variables are not supported yet
// typedef fdb_config config;
struct Config: public fdb_config {
	Config() { }
	Config(const fdb_config& cfg) : fdb_config(cfg) { }
	uint32_t getBlocksize() const { return blocksize; }
	void setBlocksize(uint32_t blocksize) { this->blocksize = blocksize; }
	uint64_t getBuffercacheSize() const { return buffercache_size; }
	void setBuffercacheSize(uint64_t buffercacheSize) { buffercache_size = buffercacheSize;}
	uint16_t getChunksize() const {return chunksize;}
	void setChunksize(uint16_t chunksize) {this->chunksize = chunksize;}
	bool isCleanupCacheOnclose() const {return cleanup_cache_onclose;}
	void setCleanupCacheOnclose(bool cleanupCacheOnclose) {cleanup_cache_onclose = cleanupCacheOnclose;}
	//fdb_custom_cmp_fixed getCmpFixed() const {return cmp_fixed;}
	//void setCmpFixed(fdb_custom_cmp_fixed cmpFixed) {cmp_fixed = cmpFixed;}
	//fdb_custom_cmp_variable getCmpVariable() const {return cmp_variable;}
	//void setCmpVariable(fdb_custom_cmp_variable cmpVariable) {cmp_variable = cmpVariable;}
	uint32_t getCompactionBufMaxsize() const {return compaction_buf_maxsize;}
	void setCompactionBufMaxsize(uint32_t compactionBufMaxsize) {compaction_buf_maxsize = compactionBufMaxsize;}
	uint64_t getCompactionMinimumFilesize() const {return compaction_minimum_filesize;}
	void setCompactionMinimumFilesize(uint64_t compactionMinimumFilesize) {compaction_minimum_filesize = compactionMinimumFilesize;}
	//fdb_compaction_mode_t getCompactionMode() const {return compaction_mode;}
	//void setCompactionMode(fdb_compaction_mode_t compactionMode) {compaction_mode = compactionMode;}
	uint8_t getCompactionThreshold() const {return compaction_threshold;}
	void setCompactionThreshold(uint8_t compactionThreshold) {compaction_threshold = compactionThreshold;}
	uint64_t getCompactorSleepDuration() const {return compactor_sleep_duration;}
	void setCompactorSleepDuration(uint64_t compactorSleepDuration) {compactor_sleep_duration = compactorSleepDuration;}
	bool isCompressDocumentBody() const {return compress_document_body;}
	void setCompressDocumentBody(bool compressDocumentBody) {compress_document_body = compressDocumentBody;}
	//fdb_durability_opt_t getDurabilityOpt() const {return durability_opt;}
	//void setDurabilityOpt(fdb_durability_opt_t durabilityOpt) {durability_opt = durabilityOpt;}
	OpenFlags getFlags() const {return (OpenFlags)flags;}
	void setFlags(OpenFlags flags) {this->flags = (fdb_open_flags)flags;}
	uint32_t getPurgingInterval() const {return purging_interval;}
	void setPurgingInterval(uint32_t purgingInterval) {purging_interval = purgingInterval;}
	//fdb_seqtree_opt_t getSeqtreeOpt() const {return seqtree_opt;}
	//void setSeqtreeOpt(fdb_seqtree_opt_t seqtreeOpt) {seqtree_opt = seqtreeOpt;}
	bool isWalFlushBeforeCommit() const {return wal_flush_before_commit;}
	void setWalFlushBeforeCommit(bool walFlushBeforeCommit) {wal_flush_before_commit = walFlushBeforeCommit;}
	uint64_t getWalThreshold() const {return wal_threshold;}
	void setWalThreshold(uint64_t walThreshold) {wal_threshold = walThreshold;}
};

}


#endif /* CBF_FORESTDB_H_ */
