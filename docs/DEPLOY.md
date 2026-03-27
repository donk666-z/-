# 部署文档

## 开发环境要求

### 后端
- JDK 8 或以上
- Maven 3.6+
- MySQL 8.0
- Redis 5.0+

### 前端
- Node.js 14+
- HBuilderX（小程序开发）
- 微信开发者工具

## 本地开发部署

### 1. 数据库初始化

```bash
# 创建数据库并导入初始数据
mysql -u root -p < database/init.sql
```

### 2. 配置后端

修改 `server/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_delivery
    username: root
    password: your_password
  
  redis:
    host: localhost
    port: 6379
    password: 

jwt:
  secret: your_jwt_secret_key

wechat:
  appid: your_wx_appid
  secret: your_wx_secret
```

### 3. 启动后端服务

```bash
cd server

# 使用 Maven 启动
mvn clean install
mvn spring-boot:run

# 或使用 IDEA
# 直接运行 CampusDeliveryApplication.java
```

服务启动后访问: `http://localhost:8080/api/health`

### 4. 配置小程序

修改 `client/utils/request.js`:

```javascript
const BASE_URL = 'http://localhost:8080/api'
```

修改 `client/manifest.json`:

```json
{
  "mp-weixin": {
    "appid": "your_wx_appid"
  }
}
```

### 5. 启动小程序

```bash
cd client
npm install

# 使用 HBuilderX 打开项目
# 运行 -> 运行到小程序模拟器 -> 微信开发者工具
```

## 生产环境部署

### 1. 后端打包

```bash
cd server
mvn clean package -DskipTests

# 生成的 jar 包位于 target/delivery-1.0.0.jar
```

### 2. 服务器部署

```bash
# 上传 jar 包到服务器
scp target/delivery-1.0.0.jar user@server:/opt/campus-delivery/

# SSH 登录服务器
ssh user@server

# 启动服务
cd /opt/campus-delivery
nohup java -jar delivery-1.0.0.jar --spring.profiles.active=prod > app.log 2>&1 &

# 查看日志
tail -f app.log
```

### 3. Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /uploads/ {
        alias /opt/campus-delivery/uploads/;
    }
}
```

### 4. 使用 Docker 部署（推荐）

创建 `Dockerfile`:

```dockerfile
FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY target/delivery-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

创建 `docker-compose.yml`:

```yaml
version: '3'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: campus_delivery
    volumes:
      - ./database/init.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"

  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"

  app:
    build: ./server
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/campus_delivery
      SPRING_REDIS_HOST: redis

volumes:
  mysql-data:
```

启动:

```bash
docker-compose up -d
```

### 5. 小程序发布

1. 在微信公众平台注册小程序
2. 修改 `client/manifest.json` 中的 `appid`
3. 修改 `client/utils/request.js` 中的 `BASE_URL` 为线上地址
4. 使用 HBuilderX 发行 -> 小程序-微信
5. 上传到微信后台审核发布

## 性能优化建议

### 后端优化

1. **数据库连接池**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

2. **Redis 缓存**
- 商户列表缓存
- 菜品信息缓存
- 骑手位置缓存

3. **异步处理**
- 订单状态推送
- 短信通知
- 日志记录

### 前端优化

1. **图片优化**
- 使用 WebP 格式
- 图片懒加载
- CDN 加速

2. **请求优化**
- 接口防抖节流
- 数据分页加载
- 本地缓存

## 监控与日志

### 日志配置

```yaml
logging:
  level:
    com.campus.delivery: info
  file:
    name: logs/app.log
    max-size: 10MB
    max-history: 30
```

### 健康检查

```bash
# 检查服务状态
curl http://localhost:8080/api/health

# 查看 JVM 信息
curl http://localhost:8080/actuator/health
```

## 常见问题

### 1. 数据库连接失败
- 检查 MySQL 是否启动
- 检查数据库用户名密码
- 检查防火墙设置

### 2. Redis 连接失败
- 检查 Redis 是否启动
- 检查 Redis 配置

### 3. 小程序请求失败
- 检查服务器域名是否在微信后台配置
- 检查 HTTPS 证书
- 检查跨域配置

### 4. WebSocket 连接失败
- 检查 Nginx WebSocket 配置
- 检查防火墙端口开放
