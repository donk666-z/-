# API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token (Header: `Authorization: Bearer {token}`)

## 1. 认证模块 (/auth)

### 1.1 微信登录
```
POST /auth/wx-login
```

**请求参数:**
```json
{
  "code": "微信登录code"
}
```

**响应:**
```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "jwt_token",
    "userInfo": {
      "id": 1,
      "openid": "xxx",
      "nickname": "用户昵称",
      "role": "student"
    },
    "role": "student"
  }
}
```

### 1.2 获取用户信息
```
GET /auth/user-info
Header: Authorization: Bearer {token}
```

## 2. 商户模块 (/merchant)

### 2.1 获取商户列表
```
GET /merchant/list?keyword=&status=
```

**查询参数:**
- `keyword`: 搜索关键词（可选）
- `status`: 营业状态（可选）

### 2.2 获取商户详情
```
GET /merchant/{id}
```

### 2.3 获取商户菜品
```
GET /merchant/{id}/dishes
```

### 2.4 更新商户信息
```
PUT /merchant/{id}
需要认证
```

### 2.5 更新营业状态
```
PUT /merchant/{id}/status
需要认证
```

## 3. 订单模块 (/order)

### 3.1 创建订单
```
POST /order/create
需要认证
```

**请求参数:**
```json
{
  "merchantId": 1,
  "items": [
    {
      "dishId": 1,
      "name": "宫保鸡丁",
      "image": "xxx.jpg",
      "price": 15.00,
      "quantity": 2
    }
  ],
  "address": "学生宿舍1号楼101",
  "phone": "13800138000",
  "remark": "少辣",
  "deliveryFee": 3.00
}
```

### 3.2 获取订单列表
```
GET /order/list?status=
需要认证
```

**查询参数:**
- `status`: 订单状态（可选）
  - `processing`: 进行中
  - `completed`: 已完成
  - `cancelled`: 已取消

### 3.3 获取订单详情
```
GET /order/{id}
需要认证
```

### 3.4 取消订单
```
POST /order/{id}/cancel
需要认证
```

### 3.5 确认收货
```
POST /order/{id}/confirm
需要认证
```

### 3.6 商户接单
```
POST /order/{id}/accept
需要认证（商户）
```

### 3.7 餐品备好
```
POST /order/{id}/prepare
需要认证（商户）
```

### 3.8 骑手取餐
```
POST /order/{id}/pickup
需要认证（骑手）
```

### 3.9 确认送达
```
POST /order/{id}/deliver
需要认证（骑手）
```

## 4. 菜品模块 (/dish)

### 4.1 创建菜品
```
POST /dish
需要认证（商户）
```

### 4.2 更新菜品
```
PUT /dish/{id}
需要认证（商户）
```

### 4.3 删除菜品
```
DELETE /dish/{id}
需要认证（商户）
```

### 4.4 更新菜品状态
```
PUT /dish/{id}/status
需要认证（商户）
```

## 5. 骑手模块 (/rider)

### 5.1 获取可接订单
```
GET /rider/orders/available
需要认证（骑手）
```

### 5.2 抢单
```
POST /rider/orders/{id}/grab
需要认证（骑手）
```

### 5.3 更新位置
```
POST /rider/location
需要认证（骑手）
```

**请求参数:**
```json
{
  "latitude": 23.123456,
  "longitude": 113.123456,
  "orderId": 1
}
```

### 5.4 获取统计数据
```
GET /rider/stats
需要认证（骑手）
```

## 6. 用户模块 (/user)

### 6.1 获取个人信息
```
GET /user/profile
需要认证
```

### 6.2 更新个人信息
```
PUT /user/profile
需要认证
```

### 6.3 获取收货地址列表
```
GET /user/addresses
需要认证
```

### 6.4 添加收货地址
```
POST /user/addresses
需要认证
```

### 6.5 更新收货地址
```
PUT /user/addresses/{id}
需要认证
```

### 6.6 删除收货地址
```
DELETE /user/addresses/{id}
需要认证
```

## 统一响应格式

### 成功响应
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {}
}
```

### 错误响应
```json
{
  "code": 500,
  "message": "错误信息",
  "data": null
}
```

## 状态码说明

- `0`: 成功
- `400`: 参数错误
- `401`: 未授权
- `403`: 无权限
- `404`: 资源不存在
- `500`: 服务器错误

## 订单状态流转

```
pending (待支付)
  ↓
paid (已支付/待接单)
  ↓
accepted (商户已接单/制作中)
  ↓
prepared (餐品已备好/待取餐)
  ↓
delivering (配送中)
  ↓
delivered (已送达)
  ↓
completed (已完成)

任意状态 → cancelled (已取消)
```
