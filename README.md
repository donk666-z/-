# 基于微信小程序的校园外卖配送系统

## 项目简介

本项目是一个完整的校园外卖配送系统，包含学生端、商户端、骑手端小程序以及管理后台。

## 技术栈

### 前端
- **小程序**: uni-app + uView UI
- **管理后台**: Vue 3 + Element Plus + Vite

### 后端
- **框架**: Spring Boot 2.7.14
- **ORM**: MyBatis Plus 3.5.3
- **数据库**: MySQL 8.0 + Redis
- **实时通信**: WebSocket (STOMP)
- **认证**: JWT
- **文件存储**: 腾讯云 COS
- **支付**: 微信支付

## 项目结构

```
campus-delivery/
├── client/          # 小程序前端（uni-app）
├── server/          # 后端服务
├── admin/           # 管理后台（Vue3）
├── database/        # 数据库脚本
└── docs/            # 项目文档
```

## 快速开始

### 1. 安装依赖

```bash
# 安装小程序依赖
cd client
npm install

# 安装后端依赖
cd ../server
npm install

# 安装管理后台依赖
cd ../admin
npm install
```

### 2. 配置环境

修改 `server/src/main/resources/application.yml` 配置文件，填写数据库和Redis连接信息。

### 3. 初始化数据库

```bash
mysql -u root -p < database/init.sql
```

### 4. 启动项目

```bash
# 启动后端服务（需要安装 Maven 和 JDK 8+）
cd server
mvn spring-boot:run

# 或使用 IDEA 直接运行 CampusDeliveryApplication

# 启动管理后台
cd admin
npm install
npm run dev

# 使用 HBuilderX 打开 client 目录运行小程序
cd client
npm install
# 然后在 HBuilderX 中运行到微信开发者工具
```

## 功能模块

### 学生端
- 食堂/餐厅浏览
- 在线菜单与下单
- 实时配送追踪
- 订单管理与评价

### 商户端
- 店铺与菜单管理
- 订单接收与处理
- 营业数据看板

### 骑手端
- 任务抢单与接单
- 智能路径规划
- 送达确认与结单

### 管理后台
- 全局用户管理
- 订单与资金监控
- 系统配置与营销

## 开发进度

- [ ] 基础架构搭建
- [ ] 学生端核心功能
- [ ] 商户端核心功能
- [ ] 骑手端核心功能
- [ ] 实时追踪系统
- [ ] 管理后台
- [ ] 增值功能
- [ ] 测试与优化

## 许可证

MIT
