USE campus_delivery;

CREATE TABLE IF NOT EXISTS promotion_activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '活动名称',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '活动状态',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    claim_limit_per_user INT NOT NULL DEFAULT 1 COMMENT '每人领取上限',
    description VARCHAR(255) NULL COMMENT '活动说明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coupon_id (coupon_id),
    INDEX idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='促销活动表';

ALTER TABLE user_coupons
    ADD COLUMN IF NOT EXISTS promotion_id BIGINT NULL COMMENT '促销活动ID' AFTER coupon_id;

INSERT INTO system_configs (config_key, config_value, description)
SELECT 'delivery_fee', '3.00', '默认配送费'
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE config_key = 'delivery_fee');

INSERT INTO system_configs (config_key, config_value, description)
SELECT 'min_order_amount', '10.00', '最低起送金额'
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE config_key = 'min_order_amount');

INSERT INTO system_configs (config_key, config_value, description)
SELECT 'platform_commission', '0.15', '平台抽成比例'
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE config_key = 'platform_commission');
