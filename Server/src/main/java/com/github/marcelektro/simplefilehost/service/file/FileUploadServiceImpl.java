package com.github.marcelektro.simplefilehost.service.file;

import com.github.marcelektro.simplefilehost.service.ServiceResult;
import com.github.marcelektro.simplefilehost.service.db.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private final File blobRoot;
    private final DatabaseService dbService;

    public FileUploadServiceImpl(File blobRoot, DatabaseService dbService) {
        this.blobRoot = blobRoot;
        this.dbService = dbService;
    }


    @Override
    public ServiceResult<String> uploadFile(String userId, String originalFilename, long size, InputStream inputStream) throws Exception {
        var fileId = UUID.randomUUID().toString();

        var dir = fileId.substring(0, 2);
        var filename = fileId.substring(2);

        var targetDir = new File(this.blobRoot, dir);

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Failed to create blob directory");
        }

        var blobFile = new File(targetDir, filename);

        if (blobFile.exists())
            throw new IllegalStateException("Duplicate file upload.");

        Files.copy(inputStream, blobFile.toPath());

        var now = LocalDateTime.now();

        try (var conn = this.dbService.getConnection()) {

            var sql = """
                        INSERT INTO uploaded_files (id, userId, filename, size, uploadDate, path)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, fileId);
                ps.setString(2, userId);
                ps.setString(3, originalFilename);
                ps.setLong(4, size);
                ps.setString(5, now.toString());
                ps.setString(6, blobFile.getAbsolutePath());
                ps.executeUpdate();
            }
        }

        return ServiceResult.success(fileId);
    }

    @Override
    public ServiceResult<FileDownloadResult> downloadByFileId(String userId, String fileId) throws Exception {
        try (var conn = this.dbService.getConnection()) {
            var sql = """
                        SELECT filename, path
                        FROM uploaded_files
                        WHERE id = ? AND userId = ?
                        """;
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, fileId);
                ps.setString(2, userId);
                var rs = ps.executeQuery();

                if (!rs.next())
                    return ServiceResult.failure("FILE_NOT_FOUND", "File not found or access denied");

                var originalFilename = rs.getString("filename");
                var path = rs.getString("path");

                var result = new FileDownloadResult(
                        originalFilename,
                        new File(path)
                );
                return ServiceResult.success(result);
            }
        }
    }

    @Override
    public ServiceResult<FileDownloadResult> downloadBySharedLink(String linkId, @Nullable String userEnteredPassword) throws Exception {
        try (var conn = this.dbService.getConnection()) {
            conn.setAutoCommit(false);

            var linkQuery = """
                            SELECT fileId, password, expiry
                            FROM shared_links
                            WHERE id = ?
                            """;
            try (var ps = conn.prepareStatement(linkQuery)) {
                ps.setString(1, linkId);
                var rs = ps.executeQuery();

                if (!rs.next())
                    return ServiceResult.failure("LINK_NOT_FOUND", "Shared link not found");

                var fileId = rs.getString("fileId");
                var password = rs.getString("password");
                var expiry = rs.getString("expiry") != null
                        ? LocalDateTime.parse(rs.getString("expiry"))
                        : null;

                if (expiry != null && expiry.isBefore(LocalDateTime.now()))
                    return ServiceResult.failure("LINK_EXPIRED", "Shared link has expired");

                if (password != null && !password.equals(userEnteredPassword))
                    return ServiceResult.failure("INVALID_PASSWORD", "Invalid password for shared link");

                // TODO: depending on whether we wanna go multi share, merge into one query?
                var fileQuery = """
                                SELECT filename, path
                                FROM uploaded_files
                                WHERE id = ?
                                """;
                try (var filePs = conn.prepareStatement(fileQuery)) {
                    filePs.setString(1, fileId);
                    var fileRs = filePs.executeQuery();

                    if (!fileRs.next())
                        throw new IllegalStateException("File not found for shared link, this should never happen since shared links can't exist without a file");

                    var result = new FileDownloadResult(
                            fileRs.getString("filename"),
                            new File(fileRs.getString("path"))
                    );

                    return ServiceResult.success(result);
                }

            } catch (Exception e) {
                conn.rollback();
                throw e;

            } finally {
                conn.commit();
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public ServiceResult<List<UploadedFileSummary>> listFiles(String userId, SortBy sortBy) throws SQLException {
        var files = new ArrayList<UploadedFileSummary>();
        var sql = """
                SELECT uf.id, uf.filename, uf.size, uf.uploadDate,
                       COUNT(sl.id) AS "sharedLinksCount"
                FROM uploaded_files uf
                    LEFT JOIN shared_links sl ON sl.fileId = uf.id
                WHERE uf.userId = ?
                GROUP BY uf.id, uf.filename, uf.size, uf.uploadDate;
                """;
        sql += switch (sortBy != null ? sortBy : SortBy.DATE_DESC) {
            case NAME_ASC -> " ORDER BY uf.filename ASC";
            case NAME_DESC -> " ORDER BY uf.filename DESC";
            case DATE_ASC -> " ORDER BY uf.uploadDate ASC";
            case DATE_DESC -> " ORDER BY uf.uploadDate DESC";
            case SIZE_ASC -> " ORDER BY uf.size ASC";
            case SIZE_DESC -> " ORDER BY uf.size DESC";
        };

        try (var conn = dbService.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            var rs = ps.executeQuery();

            while (rs.next()) {
                var summary = new UploadedFileSummary();
                summary.fileId = rs.getString("id");
                summary.filename = rs.getString("filename");
                summary.size = rs.getLong("size");
                summary.uploadDate = LocalDateTime.parse(rs.getString("uploadDate"));
                summary.sharedLinksCount = rs.getInt("sharedLinksCount");
                files.add(summary);
            }
        }

        return ServiceResult.success(files);
    }

    @Override
    public ServiceResult<Void> deleteFile(String userId, String fileId) throws Exception {
        try (var conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get file path
                String selectSql = """
                                SELECT path
                                FROM uploaded_files
                                WHERE id = ? AND userId = ?
                                """;
                String filePath;
                try (var ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, fileId);
                    ps.setString(2, userId);
                    var rs = ps.executeQuery();
                    if (rs.next()) {
                        filePath = rs.getString("path");
                    } else {
                        conn.rollback();
                        return ServiceResult.failure("FILE_NOT_FOUND", "File not found or access denied");
                    }
                }

                // Delete DB record
                String deleteSql = """
                                   DELETE FROM uploaded_files
                                   WHERE id = ? AND userId = ?
                                   """;
                try (var ps = conn.prepareStatement(deleteSql)) {
                    ps.setString(1, fileId);
                    ps.setString(2, userId);
                    ps.executeUpdate();
                }


                if (filePath == null) {
                    throw new IllegalStateException("File path is null, but file exists in database: " + fileId);
                }

                var file = new File(filePath);

                if (!file.exists()) {
                    throw new IllegalStateException("File does not exist on disk, but exists in database: " + filePath);
                }

                var deleteRes = file.delete();

                if (!deleteRes) {
                    throw new IOException("Failed to delete file from disk: " + filePath);
                }

                return ServiceResult.success(null);

            } catch (Exception e) {
                conn.rollback();
                throw e;

            } finally {
                conn.commit();
                conn.setAutoCommit(true);
            }
        }
    }
}
