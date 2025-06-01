package com.github.marcelektro.simplefilehost.service.file;

import com.github.marcelektro.simplefilehost.service.ServiceResult;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface FileUploadService {

    ServiceResult<String> uploadFile(String userId, String originalFilename, long size, InputStream inputStream) throws Exception;

    ServiceResult<FileDownloadResult> downloadByFileId(String userId, String fileId) throws Exception;

    ServiceResult<FileDownloadResult> downloadBySharedLink(String linkId, String password) throws Exception;

    ServiceResult<List<UploadedFileSummary>> listFiles(String userId, SortBy sortBy) throws SQLException;

    ServiceResult<Void> deleteFile(String userId, String fileId) throws Exception;


    enum SortBy {
        NAME_ASC,
        NAME_DESC,
        DATE_ASC,
        DATE_DESC,
        SIZE_ASC,
        SIZE_DESC
    }

    class UploadedFileSummary {
        public String fileId;
        public String filename;
        public long size;
        public LocalDateTime uploadDate;
        public int sharedLinksCount;
    }

    record FileDownloadResult(
            String originalFilename,
            File file
    ) {}
}
