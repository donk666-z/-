-- =============================================================================
-- 迁移：categories 表约束与索引升级（适用于已有库，不删表）
-- 环境要求：MySQL 8.0.16+（CHECK 约束自 8.0.16 起生效）
-- 使用前：请先备份数据库；在测试库执行一遍再用于生产。
-- =============================================================================

USE campus_delivery;

-- -----------------------------------------------------------------------------
-- 第一步（可选）：自检 —— 若下面任一有结果，须先处理数据再执行第二步
-- -----------------------------------------------------------------------------

-- 1) 同商户下分类名重复（会导致 UNIQUE 添加失败）
SELECT merchant_id, name, COUNT(*) AS cnt
FROM categories
GROUP BY merchant_id, name
HAVING cnt > 1;

-- 2) 非法排序值（会导致 CHECK 添加失败）
SELECT id, merchant_id, name, sort FROM categories WHERE sort IS NULL OR sort < 0;

-- -----------------------------------------------------------------------------
-- 第二步（可选）：数据修复示例（按需取消注释并修改）
-- -----------------------------------------------------------------------------

-- 将 sort 为 NULL 的归为 0（若你希望 NULL 表示 0）
-- UPDATE categories SET sort = 0 WHERE sort IS NULL;

-- 将负数 sort 修正为 0（或按业务改为其他非负数）
-- UPDATE categories SET sort = 0 WHERE sort < 0;

-- 重复名称处理示例：保留每组 (merchant_id, name) 中 id 最小的一条，
-- 其余在名称末尾追加 _{id}（注意 name 字段长度 50，极长名称需自行截断）
-- UPDATE categories c
-- INNER JOIN (
--   SELECT merchant_id, name, MIN(id) AS keep_id
--   FROM categories
--   GROUP BY merchant_id, name
--   HAVING COUNT(*) > 1
-- ) dup ON c.merchant_id = dup.merchant_id
--        AND c.name = dup.name
--        AND c.id > dup.keep_id
-- SET c.name = LEFT(CONCAT(c.name, '_', c.id), 50);

-- -----------------------------------------------------------------------------
-- 第三步：结构变更（按你库中实际索引名调整；若某步报错“不存在”可跳过该步）
-- -----------------------------------------------------------------------------

-- 若建表时存在单列商户索引 idx_merchant，可与新索引冗余；删除可减少维护成本。
-- 若不存在该索引，本句会报错，直接忽略或注释掉即可。
ALTER TABLE categories DROP INDEX idx_merchant;

-- 唯一约束：同一商户下分类名不可重复
ALTER TABLE categories
  ADD UNIQUE KEY uk_merchant_name (merchant_id, name);

-- 列表排序：与后端 list 查询一致 (merchant_id, sort, id)
ALTER TABLE categories
  ADD INDEX idx_merchant_sort (merchant_id, sort, id);

-- 排序非负（与 CategoryServiceImpl 校验一致）
ALTER TABLE categories
  ADD CONSTRAINT chk_categories_sort_nonnegative CHECK (sort >= 0);

-- -----------------------------------------------------------------------------
-- 验证（执行后检查）
-- -----------------------------------------------------------------------------
-- SHOW CREATE TABLE categories\G
-- SHOW INDEX FROM categories;
