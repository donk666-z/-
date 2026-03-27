-- 创建投诉建议表
CREATE TABLE IF NOT EXISTS `complaint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` VARCHAR(50) NOT NULL COMMENT '投诉类型：商品问题、配送问题、服务态度、其他',
    `content` TEXT NOT NULL COMMENT '投诉内容',
    `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系方式',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-处理中、resolved-已解决、closed-已关闭',
    `reply` TEXT DEFAULT NULL COMMENT '回复内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉建议表';
