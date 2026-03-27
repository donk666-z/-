USE campus_delivery;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password VARCHAR(100) NULL COMMENT '密码' AFTER phone;
