package com.campus.delivery.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaCompatibilityRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("users", "password",
                "ALTER TABLE users ADD COLUMN password VARCHAR(100) NULL COMMENT '密码' AFTER phone");

        ensureColumn("dishes", "dish_type",
                "ALTER TABLE dishes ADD COLUMN dish_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '菜品类型' AFTER status");
        ensureColumn("dishes", "combo_config",
                "ALTER TABLE dishes ADD COLUMN combo_config JSON NULL COMMENT '套餐配置' AFTER dish_type");

        ensureColumn("order_items", "item_type",
                "ALTER TABLE order_items ADD COLUMN item_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '订单项类型' AFTER quantity");
        ensureColumn("order_items", "combo_snapshot",
                "ALTER TABLE order_items ADD COLUMN combo_snapshot JSON NULL COMMENT '套餐快照' AFTER item_type");
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        if (!tableExists(tableName)) {
            return;
        }
        if (columnExists(tableName, columnName)) {
            return;
        }
        log.warn("Detected legacy schema: adding missing column {}.{}", tableName, columnName);
        jdbcTemplate.execute(alterSql);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
