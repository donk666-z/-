USE campus_delivery;

ALTER TABLE dishes
    ADD COLUMN dish_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '菜品类型' AFTER status,
    ADD COLUMN combo_config JSON NULL COMMENT '套餐配置' AFTER dish_type;

ALTER TABLE order_items
    ADD COLUMN item_type ENUM('single', 'combo') NOT NULL DEFAULT 'single' COMMENT '订单项类型' AFTER quantity,
    ADD COLUMN combo_snapshot JSON NULL COMMENT '套餐快照' AFTER item_type;
