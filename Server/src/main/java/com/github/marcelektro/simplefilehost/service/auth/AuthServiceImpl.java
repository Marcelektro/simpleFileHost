package com.github.marcelektro.simplefilehost.service.auth;

import com.github.marcelektro.simplefilehost.service.ServiceResult;
import com.github.marcelektro.simplefilehost.service.db.DatabaseService;
import com.github.marcelektro.simplefilehost.util.Checks;
import com.github.marcelektro.simplefilehost.util.HashUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;

public class AuthServiceImpl implements AuthService {

    private final DatabaseService dbService;
    private final SecretKey jwtKey;

    public AuthServiceImpl(DatabaseService dbService, String jwtSecret) {
        this.dbService = dbService;
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }


    @Override
    public ServiceResult<String> registerUser(String id, String username, String password) throws Exception {
        if (Checks.empty(username) || Checks.empty(password)) {
            return ServiceResult.failure("INVALID_INPUT", "Username and password must not be empty");
        }

        try (final var conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                var stmt = conn.prepareStatement("""
                                                SELECT 1
                                                FROM users
                                                WHERE username = ? OR id = ?
                                                """);
                stmt.setString(1, username);
                stmt.setString(2, id);
                var rs = stmt.executeQuery();

                if (rs.next()) {
                    return ServiceResult.failure("USERNAME_OR_ID_TAKEN", "Username or id is already taken");
                }

                var salt = HashUtil.generateSalt();
                var passwordHash = HashUtil.hashPassword(password, salt);

                stmt = conn.prepareStatement("""
                                                INSERT INTO users (id, username, passwordHash, passwordSalt)
                                                VALUES (?, ?, ?, ?)
                                                """);
                stmt.setString(1, id);
                stmt.setString(2, username);
                stmt.setString(3, passwordHash);
                stmt.setString(4, salt);
                stmt.executeUpdate();

                return ServiceResult.success(id);

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
    public ServiceResult<AuthResult> login(String username, String password) throws Exception {
        try (final var conn = dbService.getConnection()) {
            var stmt = conn.prepareStatement("""
                                            SELECT users.id, users.username, users.passwordHash, users.passwordSalt
                                            FROM users
                                            WHERE users.username = ?
                                            """);
            stmt.setString(1, username);
            var rs = stmt.executeQuery();

            if (!rs.next()) {
                return ServiceResult.failure("INVALID_CREDENTIALS", "Invalid username or password");
            }

            var userId = rs.getString("id");
            var userName = rs.getString("username");
            var passwordHash = rs.getString("passwordHash");
            var passwordSalt = rs.getString("passwordSalt");

            if (!HashUtil.verifyPassword(password, passwordHash, passwordSalt)) {
                return ServiceResult.failure("INVALID_CREDENTIALS", "Invalid username or password");
            }

            var token = generateToken(userId);

            var result = new AuthResult(
                    Integer.parseInt(userId),
                    token,
                    userName
            );

            return ServiceResult.success(result);

        } catch (SQLException e) {
            throw new Exception("Database error during login", e);
        }
    }

    @Override
    public ServiceResult<Integer> validateTokenAndGetUserId(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(this.jwtKey)
                    .build()
                    .parseSignedClaims(token);

            var userId = Integer.parseInt(claims.getPayload().getSubject());

            return ServiceResult.success(userId);

        } catch (JwtException e) { // TODO: Better error handling

            if (e instanceof ExpiredJwtException)
                return ServiceResult.failure("EXPIRED_TOKEN", "Token expired");

            return ServiceResult.failure("TOKEN_VALIDATION_FAILURE", "Token validation failed: " + e.getMessage());
        }
    }

    private String generateToken(String userId) {
        var now = Instant.now();

        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600))) // 1h expiration
                .signWith(this.jwtKey)
                .compact();
    }

}
