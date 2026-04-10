SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS campus_delivery;
CREATE DATABASE campus_delivery DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_delivery;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(100) NOT NULL UNIQUE COMMENT '微信openid',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像',
    phone VARCHAR(11) COMMENT '手机号',
    password VARCHAR(100) COMMENT '密码',
    role ENUM('student', 'merchant', 'rider', 'admin') DEFAULT 'student' COMMENT '角色',
    status ENUM('active', 'inactive', 'banned') DEFAULT 'active' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE merchants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '关联用户ID',
    name VARCHAR(100) NOT NULL COMMENT '商户名称',
    logo VARCHAR(255) COMMENT '商户logo',
    description VARCHAR(255) COMMENT '描述',
    address VARCHAR(255) COMMENT '地址',
    latitude DECIMAL(10,6) COMMENT '纬度',
    longitude DECIMAL(10,6) COMMENT '经度',
    phone VARCHAR(11) COMMENT '联系电话',
    business_hours VARCHAR(100) COMMENT '营业时间',
    status ENUM('open', 'closed', 'busy') DEFAULT 'open' COMMENT '营业状态',
    month_sales INT DEFAULT 0 COMMENT '月销量',
    rating DECIMAL(3,2) DEFAULT 5.0 COMMENT '评分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_name (name),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';

CREATE TABLE riders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(100) NOT NULL UNIQUE COMMENT '微信openid',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    phone VARCHAR(11) NOT NULL COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像',
    status ENUM('online', 'offline', 'delivering') DEFAULT 'offline' COMMENT '状态',
    latitude DECIMAL(10,6) COMMENT '当前纬度',
    longitude DECIMAL(10,6) COMMENT '当前经度',
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    total_income DECIMAL(10,2) DEFAULT 0 COMMENT '总收入',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手表';

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL COMMENT '商户ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_name (merchant_id, name),
    INDEX idx_merchant_sort (merchant_id, sort, id),
    CONSTRAINT chk_categories_sort_nonnegative CHECK (sort >= 0),
    FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

CREATE TABLE dishes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL COMMENT '商户ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '菜品名称',
    description VARCHAR(255) COMMENT '描述',
    image VARCHAR(255) COMMENT '图片',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 999 COMMENT '库存',
    sales INT DEFAULT 0 COMMENT '销量',
    status ENUM('available', 'unavailable') DEFAULT 'available' COMMENT '状态',
    dish_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '菜品类型',
    combo_config JSON NULL COMMENT '套餐配置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

CREATE TABLE coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type ENUM('discount', 'deduction') NOT NULL COMMENT '类型',
    discount DECIMAL(10,2) COMMENT '折扣值或减免金额',
    min_amount DECIMAL(10,2) COMMENT '最低消费金额',
    total INT NOT NULL COMMENT '总数量',
    claimed INT DEFAULT 0 COMMENT '已领取数量',
    start_time TIMESTAMP NULL COMMENT '开始时间',
    end_time TIMESTAMP NULL COMMENT '结束时间',
    status ENUM('active', 'inactive') DEFAULT 'active' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

CREATE TABLE addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '收货人',
    phone VARCHAR(11) NOT NULL COMMENT '联系电话',
    address VARCHAR(255) NOT NULL COMMENT '详细地址',
    building VARCHAR(50) COMMENT '楼栋',
    room VARCHAR(50) COMMENT '房间号',
    latitude DECIMAL(10,6) COMMENT '纬度',
    longitude DECIMAL(10,6) COMMENT '经度',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址表';

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    merchant_id BIGINT NOT NULL COMMENT '商户ID',
    rider_id BIGINT COMMENT '骑手ID',
    dish_price DECIMAL(10,2) NOT NULL COMMENT '菜品总价',
    delivery_fee DECIMAL(10,2) DEFAULT 0 COMMENT '配送费',
    total_price DECIMAL(10,2) NOT NULL COMMENT '总价',
    address VARCHAR(255) NOT NULL COMMENT '配送地址',
    phone VARCHAR(11) NOT NULL COMMENT '联系电话',
    remark VARCHAR(255) COMMENT '备注',
    status ENUM('pending', 'paid', 'accepted', 'prepared', 'delivering', 'delivered', 'completed', 'cancelled') DEFAULT 'pending' COMMENT '订单状态',
    estimated_time INT COMMENT '预计送达时间(分钟)',
    payment_method VARCHAR(20) COMMENT '支付方式',
    payment_time TIMESTAMP NULL COMMENT '支付时间',
    completed_time TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_user (user_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_rider (rider_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    FOREIGN KEY (rider_id) REFERENCES riders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    dish_id BIGINT NOT NULL COMMENT '菜品ID',
    dish_name VARCHAR(100) NOT NULL COMMENT '菜品名称',
    dish_image VARCHAR(255) COMMENT '菜品图片',
    price DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity INT NOT NULL COMMENT '数量',
    item_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '订单项类型',
    combo_snapshot JSON NULL COMMENT '套餐快照',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

CREATE TABLE order_status_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态日志表';

CREATE TABLE reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    merchant_id BIGINT NOT NULL COMMENT '商户ID',
    food_rating INT NOT NULL COMMENT '菜品评分(1-5)',
    delivery_rating INT NOT NULL COMMENT '配送评分(1-5)',
    content TEXT COMMENT '评价内容',
    images TEXT COMMENT '图片(JSON数组)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    INDEX idx_merchant (merchant_id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

CREATE TABLE user_coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    status ENUM('unused', 'used', 'expired') DEFAULT 'unused' COMMENT '状态',
    used_time TIMESTAMP NULL COMMENT '使用时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

CREATE TABLE transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT COMMENT '商户ID',
    order_id BIGINT COMMENT '订单ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    type ENUM('income', 'withdraw', 'refund') NOT NULL COMMENT '类型',
    status ENUM('pending', 'completed', 'failed') DEFAULT 'pending' COMMENT '状态',
    remark VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id),
    INDEX idx_order (order_id),
    FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表';

CREATE TABLE system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    description VARCHAR(255) COMMENT '描述',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE complaint (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(50) NOT NULL COMMENT '投诉类型',
    content TEXT NOT NULL COMMENT '投诉内容',
    contact VARCHAR(50) DEFAULT NULL COMMENT '联系方式',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
    reply TEXT DEFAULT NULL COMMENT '回复内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉建议表';

INSERT INTO system_configs (config_key, config_value, description) VALUES
('delivery_fee', '3.00', '默认配送费'),
('min_order_amount', '10.00', '最低起送金额'),
('platform_commission', '0.15', '平台抽成比例');

CREATE VIEW v_order_detail AS
SELECT
    o.id,
    o.order_no,
    o.status,
    o.total_price,
    o.address,
    o.created_at,
    u.nickname AS user_name,
    u.phone AS user_phone,
    m.name AS merchant_name,
    m.logo AS merchant_logo,
    r.name AS rider_name,
    r.phone AS rider_phone
FROM orders o
LEFT JOIN users u ON o.user_id = u.id
LEFT JOIN merchants m ON o.merchant_id = m.id
LEFT JOIN riders r ON o.rider_id = r.id;

DELIMITER //
CREATE PROCEDURE update_merchant_sales()
BEGIN
    UPDATE merchants m
    SET month_sales = (
        SELECT COALESCE(COUNT(*), 0)
        FROM orders o
        WHERE o.merchant_id = m.id
          AND o.status = 'completed'
          AND o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    );
END //
DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;
