-- 校园外卖配送系统 v2 数据库迁移脚本
USE campus_delivery;

-- 商户表增加 user_id 字段（关联用户表）
ALTER TABLE merchants ADD COLUMN user_id BIGINT COMMENT '关联用户ID' AFTER id;
ALTER TABLE merchants ADD INDEX idx_user_id (user_id);

-- 用户表增加 password 字段（仅管理员使用）
ALTER TABLE users ADD COLUMN password VARCHAR(100) COMMENT '密码(仅管理员)' AFTER phone;

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    description VARCHAR(255) COMMENT '描述',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认系统配置
INSERT INTO system_configs (config_key, config_value, description) VALUES
('delivery_fee', '3.00', '默认配送费(元)'),
('min_order_amount', '10.00', '最低起送金额(元)'),
('platform_commission', '0.15', '平台抽成比例');

-- 设置管理员密码 (明文: admin123)
UPDATE users SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' WHERE role = 'admin';

-- 更新测试商户关联用户
UPDATE merchants SET user_id = 1 WHERE id = 1;
UPDATE merchants SET user_id = 1 WHERE id = 2;

COMMIT;
