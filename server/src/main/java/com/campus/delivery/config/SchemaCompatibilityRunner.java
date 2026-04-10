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

        ensureTable("promotion_activities",
                "CREATE TABLE IF NOT EXISTS promotion_activities ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "name VARCHAR(100) NOT NULL COMMENT '活动名称',"
                        + "coupon_id BIGINT NOT NULL COMMENT '优惠券ID',"
                        + "status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '活动状态',"
                        + "start_time DATETIME NOT NULL COMMENT '开始时间',"
                        + "end_time DATETIME NOT NULL COMMENT '结束时间',"
                        + "claim_limit_per_user INT NOT NULL DEFAULT 1 COMMENT '每人领取上限',"
                        + "description VARCHAR(255) NULL COMMENT '活动说明',"
                        + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                        + "INDEX idx_coupon_id (coupon_id),"
                        + "INDEX idx_status_time (status, start_time, end_time)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='促销活动表'");

        ensureColumn("user_coupons", "promotion_id",
                "ALTER TABLE user_coupons ADD COLUMN promotion_id BIGINT NULL COMMENT '促销活动ID' AFTER coupon_id");

        ensureColumn("dishes", "dish_type",
                "ALTER TABLE dishes ADD COLUMN dish_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '菜品类型' AFTER status");
        ensureColumn("dishes", "combo_config",
                "ALTER TABLE dishes ADD COLUMN combo_config JSON NULL COMMENT '套餐配置' AFTER dish_type");

        ensureColumn("order_items", "item_type",
                "ALTER TABLE order_items ADD COLUMN item_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '订单项类型' AFTER quantity");
        ensureColumn("order_items", "combo_snapshot",
                "ALTER TABLE order_items ADD COLUMN combo_snapshot JSON NULL COMMENT '套餐快照' AFTER item_type");

        ensureConfig("delivery_fee", "3.00", "默认配送费");
        ensureConfig("min_order_amount", "10.00", "最低起送金额");
        ensureConfig("platform_commission", "0.15", "平台抽成比例");
    }

    private void ensureTable(String tableName, String createSql) {
        if (tableExists(tableName)) {
            return;
        }
        log.warn("Detected missing table {}. Creating compatible table.", tableName);
        jdbcTemplate.execute(createSql);
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

    private void ensureConfig(String key, String value, String description) {
        if (!tableExists("system_configs")) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_configs WHERE config_key = ?",
                Integer.class,
                key
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO system_configs (config_key, config_value, description) VALUES (?, ?, ?)",
                key,
                value,
                description
        );
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
