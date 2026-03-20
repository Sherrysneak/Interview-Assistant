package interview.guide.infrastructure.db;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 兼容旧数据库结构的启动迁移。
 *
 * 当前项目使用 hibernate ddl-auto=update，部分约束放宽（如 NOT NULL -> NULL）
 * 在 PostgreSQL 上不会被自动应用。这里通过幂等 SQL 做平滑升级，避免线上旧库启动后
 * 出现功能可用但关键链路写库失败的问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaCompatibilityInitializer {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void applyCompatibilityMigrations() {
        if (!isPostgres()) {
            return;
        }

        executeIgnoreError("ALTER TABLE interview_sessions ALTER COLUMN resume_id DROP NOT NULL");

        executeIgnoreError("ALTER TABLE job_descriptions ADD COLUMN IF NOT EXISTS parse_retry_count integer");
        executeIgnoreError("UPDATE job_descriptions SET parse_retry_count = 0 WHERE parse_retry_count IS NULL");
        executeIgnoreError("ALTER TABLE job_descriptions ALTER COLUMN parse_retry_count SET DEFAULT 0");
        executeIgnoreError("ALTER TABLE job_descriptions ALTER COLUMN parse_retry_count SET NOT NULL");
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProduct = connection.getMetaData().getDatabaseProductName();
            return databaseProduct != null
                && databaseProduct.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException e) {
            log.warn("检测数据库类型失败，跳过兼容迁移: {}", e.getMessage());
            return false;
        }
    }

    private void executeIgnoreError(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("兼容迁移已执行: {}", sql);
        } catch (Exception e) {
            log.warn("兼容迁移执行失败(可忽略): {}, error={}", sql, e.getMessage());
        }
    }
}
