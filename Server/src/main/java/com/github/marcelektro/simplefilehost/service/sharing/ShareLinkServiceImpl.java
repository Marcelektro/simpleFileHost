package com.github.marcelektro.simplefilehost.service.sharing;

import com.github.marcelektro.simplefilehost.service.ServiceResult;
import com.github.marcelektro.simplefilehost.service.db.DatabaseService;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShareLinkServiceImpl implements ShareLinkService {

    private final DatabaseService dbService;

    public ShareLinkServiceImpl(DatabaseService dbService) {
        this.dbService = dbService;
    }


    @Override
    public ServiceResult<String> createShareLink(String userId, String fileId, @Nullable String password, @Nullable LocalDateTime expiry) throws Exception {

        var linkId = UUID.randomUUID().toString()
                .substring(0, 8);

        try (var conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!ownsFile(conn, userId, fileId)) {
                    return ServiceResult.failure("ACCESS_DENIED", "User does not own the file");
                }

                var sql = """
                      INSERT INTO shared_links (id, fileId, expiry, password)
                      VALUES (?, ?, ?, ?)
                      """;
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, linkId);
                    ps.setString(2, fileId);
                    ps.setString(3, expiry != null ? expiry.toString() : null);
                    ps.setString(4, password);
                    ps.executeUpdate();
                }

                return ServiceResult.success(linkId);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.commit();
            }
        }
    }

    @Override
    public ServiceResult<Void> updateShareLink(String userId, String linkId, @Nullable String newPassword, @Nullable LocalDateTime newExpiry) throws Exception {
        try (var conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!ownsLink(conn, userId, linkId)) {
                    return ServiceResult.failure("ACCESS_DENIED", "User does not own this share link");
                }

                var sql = """
                      UPDATE shared_links
                      SET expiry = ?, password = ?
                      WHERE id = ?
                      """;
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newExpiry != null ? newExpiry.toString() : null);
                    ps.setString(2, newPassword);
                    ps.setString(3, linkId);
                    ps.executeUpdate();
                }

                return ServiceResult.success(null);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.commit();
            }
        }
    }

    @Override
    public ServiceResult<Void> deleteShareLink(String userId, String linkId) throws Exception {
        try (var conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!ownsLink(conn, userId, linkId)) {
                    return ServiceResult.failure("ACCESS_DENIED", "User does not own this share link");
                }

                var sql = """
                          DELETE FROM shared_links
                          WHERE id = ?
                          """;
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, linkId);
                    ps.executeUpdate();
                }

                return ServiceResult.success(null);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.commit();
            }
        }
    }

    @Override
    public ServiceResult<ShareLinkValidationResult> validateLink(String linkId, @Nullable String userEnteredPassword) throws Exception {
        try (var conn = dbService.getConnection()) {
            var sql = """
                        SELECT sl.fileId, sl.password, sl.expiry,
                               uf.filename AS "ufFilename", uf.size AS "ufSize"
                        FROM shared_links sl
                            INNER JOIN uploaded_files uf on uf.id = sl.fileId
                        WHERE sl.id = ?
                        """;
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, linkId);
                var rs = ps.executeQuery();

                if (!rs.next()) {
                    return ServiceResult.failure("LINK_NOT_FOUND", "Shared link not found");
                }

                var fileId = rs.getString("fileId");
                var password = rs.getString("password");
                var expiryStr = rs.getString("expiry");

                var expiry = expiryStr != null ? LocalDateTime.parse(expiryStr) : null;

                var hasExpiry = expiry != null;
                var hasExpired = hasExpiry && expiry.isBefore(LocalDateTime.now());

                var hasPassword = password != null && !password.isEmpty();
                var validPassword = hasPassword && password.equals(userEnteredPassword);

                var originalFilename = rs.getString("ufFilename");
                var fileSize = rs.getLong("ufSize");

                var result = new ShareLinkValidationResult(
                        fileId,
                        originalFilename,
                        fileSize,
                        hasPassword,
                        validPassword,
                        expiry,
                        hasExpired
                );

                return ServiceResult.success(result);
            }
        }
    }

    public ServiceResult<List<ShareLinkInfo>> listShareLinksForFile(String userId, String fileId) throws Exception {
        var results = new ArrayList<ShareLinkInfo>();
        try (var conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!ownsFile(conn, userId, fileId))
                    return ServiceResult.failure("ACCESS_DENIED", "User does not own the file");

                var sql = """
                            SELECT id, expiry, password
                            FROM shared_links
                            WHERE fileId = ?
                            """;
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, fileId);
                    var rs = ps.executeQuery();
                    while (rs.next()) {
                        var summary = new ShareLinkInfo(
                                rs.getString("id"),
                                rs.getString("expiry") != null ? LocalDateTime.parse(rs.getString("expiry")) : null,
                                rs.getString("password")
                        );
                        results.add(summary);
                    }
                }

                conn.commit();
                return ServiceResult.success(results);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private boolean ownsFile(Connection conn, String userId, String fileId) throws SQLException {
        var sql = """
                SELECT id
                FROM uploaded_files
                WHERE id = ? AND userId = ?
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileId);
            ps.setString(2, userId);
            return ps.executeQuery().next();
        }
    }

    private boolean ownsLink(Connection conn, String userId, String linkId) throws SQLException {

        var sql = """
            SELECT sl.id
            FROM shared_links sl
            INNER JOIN uploaded_files uf ON sl.fileId = uf.id
            WHERE sl.id = ? AND uf.userId = ?
        """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, linkId);
            ps.setString(2, userId);
            return ps.executeQuery().next();
        }

    }
}
