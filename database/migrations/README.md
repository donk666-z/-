# 数据库迁移说明

## `20260321_categories_constraints.sql`

为**已有** `campus_delivery` 库中的 `categories` 表增加：

- 唯一约束：`(merchant_id, name)`
- 列表索引：`(merchant_id, sort, id)`
- 检查约束：`sort >= 0`（需 MySQL **8.0.16+**）

### 使用方式

1. **先备份**数据库。
2. 在 Navicat / 命令行中打开脚本，**按段执行**（若某条 `DROP INDEX` 报错“索引不存在”，注释掉该条后继续）。
3. 执行前运行脚本内「第一步」的注释 SQL，确认无重复名称、无非法 `sort`。
4. 若有脏数据，按需取消注释「第二步」修复语句后再执行「第三步」。

### 与全量脚本的关系

全新环境仍可直接执行根目录 `database/init.sql`（已包含同等约束）。本迁移仅用于**线上或本地已建表**的增量升级。
