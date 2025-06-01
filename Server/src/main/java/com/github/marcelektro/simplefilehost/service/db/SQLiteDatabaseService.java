package com.github.marcelektro.simplefilehost.service.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class SQLiteDatabaseService implements DatabaseService {

    private final HikariDataSource dataSource;


    public SQLiteDatabaseService(String dbFilePath) throws Exception {
        try {
            final var config = new HikariConfig();

            config.setJdbcUrl("jdbc:sqlite:" + dbFilePath);
            config.setMaximumPoolSize(10);
            config.setPoolName("SimpleFileHostDBPool");
            config.setConnectionTestQuery("SELECT 1");

            this.dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            throw new Exception("Failed to initialize HikariCP with SQLite", e);
        }
    }


    @Override
    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }


    @Override
    public void initialSetup() throws SQLException {
        try (final var conn = this.getConnection();
             final var stmt = conn.createStatement()) {

            // User
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    passwordHash TEXT NOT NULL,
                    passwordSalt TEXT NOT NULL
                );
            """);

            // FileUpload
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS uploaded_files (
                    id TEXT PRIMARY KEY,
                    userId TEXT NOT NULL,
                    filename TEXT NOT NULL,
                    size INTEGER,
                    uploadDate TEXT NOT NULL,
                    path TEXT NOT NULL,
            
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // SharedLink
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS shared_links (
                    id TEXT PRIMARY KEY,
                    fileId TEXT NOT NULL,
                    expiry TEXT,
                    password TEXT,
            
                    FOREIGN KEY (fileId) REFERENCES uploaded_files(id) ON DELETE CASCADE
                );
            """);

            log.info("Database tables created successfully.");

        } catch (SQLException e) {
            log.error("Failed to create database tables", e);
            throw e;
        }
    }

}
