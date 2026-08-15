package com.example.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan({
        "com.example.backend.article.mapper",
        "com.example.backend.auth.audit",
        "com.example.backend.competition.mapper",
        "com.example.backend.portal.mapper",
        "com.example.backend.project.mapper",
        "com.example.backend.qna.mapper",
        "com.example.backend.resource.mapper",
        "com.example.backend.share.mapper",
        "com.example.backend.tool.mapper",
        "com.example.backend.user.mapper"
})
@EnableTransactionManagement
public class BackendApplication {
    private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner dbMigrate(JdbcTemplate jdbc) {
        return args -> {
            migrateUsers(jdbc);
            migrateContentTables(jdbc);
            createAuthLoginLogs(jdbc);
            createInteractionTables(jdbc);
            normalizeExistingShareRows(jdbc);
        };
    }

    private static void migrateUsers(JdbcTemplate jdbc) {
        addColumnIfMissing(jdbc, "users", "college", "ALTER TABLE users ADD COLUMN college VARCHAR(100) DEFAULT NULL");
        addColumnIfMissing(jdbc, "users", "email", "ALTER TABLE users ADD COLUMN email VARCHAR(100) DEFAULT NULL");
        addColumnIfMissing(jdbc, "users", "phone", "ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT NULL");
        addColumnIfMissing(jdbc, "users", "bio", "ALTER TABLE users ADD COLUMN bio TEXT DEFAULT NULL");
        addColumnIfMissing(jdbc, "users", "hic", "ALTER TABLE users ADD COLUMN hic TINYINT DEFAULT 0");
        addColumnIfMissing(jdbc, "users", "role", "ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user'");
        executeIgnoreError(jdbc, "CREATE UNIQUE INDEX uk_users_email ON users(email)");
        executeIgnoreError(jdbc, "CREATE UNIQUE INDEX uk_users_phone ON users(phone)");
        executeIgnoreError(jdbc, "CREATE UNIQUE INDEX uk_users_student_id ON users(student_id)");
    }

    private static void migrateContentTables(JdbcTemplate jdbc) {
        addColumnIfMissing(jdbc, "articles", "source", "ALTER TABLE articles ADD COLUMN source VARCHAR(20) DEFAULT NULL");
        addColumnIfMissing(jdbc, "resources", "content_url", "ALTER TABLE resources ADD COLUMN content_url VARCHAR(500) DEFAULT NULL");
        addColumnIfMissing(jdbc, "resources", "source", "ALTER TABLE resources ADD COLUMN source VARCHAR(20) DEFAULT NULL");
        addColumnIfMissing(jdbc, "resources", "tags", "ALTER TABLE resources ADD COLUMN tags VARCHAR(255) DEFAULT NULL");

        addColumnIfMissing(jdbc, "shares", "tags", "ALTER TABLE shares ADD COLUMN tags VARCHAR(255) DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "author_id", "ALTER TABLE shares ADD COLUMN author_id BIGINT DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "ai_summary", "ALTER TABLE shares ADD COLUMN ai_summary VARCHAR(255) DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "status", "ALTER TABLE shares ADD COLUMN status VARCHAR(20) DEFAULT 'published'");
        addColumnIfMissing(jdbc, "shares", "content_object_key", "ALTER TABLE shares ADD COLUMN content_object_key VARCHAR(500) DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "content_etag", "ALTER TABLE shares ADD COLUMN content_etag VARCHAR(128) DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "content_size", "ALTER TABLE shares ADD COLUMN content_size BIGINT DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "content_sha256", "ALTER TABLE shares ADD COLUMN content_sha256 VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "publish_time", "ALTER TABLE shares ADD COLUMN publish_time DATETIME DEFAULT NULL");
        addColumnIfMissing(jdbc, "shares", "updated_at", "ALTER TABLE shares ADD COLUMN updated_at DATETIME DEFAULT NULL");
        executeIgnoreError(jdbc, "CREATE INDEX idx_shares_status_publish ON shares(status, publish_time)");
        executeIgnoreError(jdbc, "CREATE INDEX idx_shares_author_status ON shares(author_id, status)");
    }

    private static void createAuthLoginLogs(JdbcTemplate jdbc) {
        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS auth_login_logs ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "user_id BIGINT DEFAULT NULL,"
                + "identifier_type VARCHAR(20) DEFAULT NULL,"
                + "identifier VARCHAR(120) DEFAULT NULL,"
                + "channel VARCHAR(30) DEFAULT NULL,"
                + "ip_address VARCHAR(64) DEFAULT NULL,"
                + "user_agent VARCHAR(500) DEFAULT NULL,"
                + "status VARCHAR(20) DEFAULT NULL,"
                + "failure_reason VARCHAR(255) DEFAULT NULL,"
                + "login_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "KEY idx_user_id (user_id),"
                + "KEY idx_login_time (login_time),"
                + "KEY idx_identifier_type (identifier_type)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }

    private static void createInteractionTables(JdbcTemplate jdbc) {
        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS comments ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "resource_id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "parent_id BIGINT DEFAULT NULL,"
                + "content TEXT NOT NULL,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "KEY idx_resource_id (resource_id),"
                + "KEY idx_user_id (user_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS points ("
                + "user_id BIGINT NOT NULL PRIMARY KEY,"
                + "balance INT NOT NULL DEFAULT 0,"
                + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "KEY idx_balance (balance)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS point_logs ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "user_id BIGINT NOT NULL,"
                + "amount INT NOT NULL,"
                + "reason VARCHAR(200) DEFAULT NULL,"
                + "ref_type VARCHAR(64) DEFAULT NULL,"
                + "ref_id BIGINT DEFAULT NULL,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "KEY idx_user_id (user_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS resource_likes ("
                + "resource_id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (resource_id, user_id),"
                + "KEY idx_resource_id (resource_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS qna_questions ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "user_id BIGINT NOT NULL,"
                + "title VARCHAR(300) NOT NULL,"
                + "content TEXT NOT NULL,"
                + "tags VARCHAR(255) DEFAULT NULL,"
                + "view_count INT DEFAULT 0,"
                + "answer_count INT DEFAULT 0,"
                + "accepted_answer_id BIGINT DEFAULT NULL,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "KEY idx_user_id (user_id),"
                + "KEY idx_created_at (created_at)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS qna_answers ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "question_id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "content TEXT NOT NULL,"
                + "like_count INT DEFAULT 0,"
                + "is_accepted TINYINT DEFAULT 0,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "KEY idx_question_id (question_id),"
                + "KEY idx_user_id (user_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        executeIgnoreError(jdbc, "CREATE TABLE IF NOT EXISTS qna_answer_likes ("
                + "answer_id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (answer_id, user_id),"
                + "KEY idx_answer_id (answer_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }

    private static void normalizeExistingShareRows(JdbcTemplate jdbc) {
        executeIgnoreError(jdbc, "UPDATE shares SET status = 'published' WHERE status IS NULL OR status = ''");
        executeIgnoreError(jdbc, "UPDATE shares SET publish_time = COALESCE(publish_time, created_at, NOW()) WHERE status = 'published'");
        executeIgnoreError(jdbc, "UPDATE shares SET updated_at = COALESCE(updated_at, created_at, NOW()) WHERE updated_at IS NULL");
    }

    private static void addColumnIfMissing(JdbcTemplate jdbc, String tableName, String columnName, String alterSql) {
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, tableName, columnName);
            if (count == null || count == 0) jdbc.execute(alterSql);
        } catch (Exception e) {
            log.warn("Database migration skipped for {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }

    private static void executeIgnoreError(JdbcTemplate jdbc, String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception e) {
            log.debug("SQL skipped: {}", e.getMessage());
        }
    }
}
