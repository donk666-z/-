-- 插入商家数据
INSERT INTO merchants (user_id, name, logo, description, address, latitude, longitude, phone, business_hours, status, month_sales, rating, created_at, updated_at)
VALUES
(1, '校园美食广场', 'https://example.com/logo1.jpg', '提供各种校园美食，价格实惠，味道正宗', '校园中心广场', 39.9042, 116.4074, '13800138001', '09:00-22:00', 'OPEN', 1200, 4.8, NOW(), NOW()),
(2, '快乐汉堡', 'https://example.com/logo2.jpg', '专注于汉堡和薯条，口感一流', '学生活动中心', 39.9043, 116.4075, '13800138002', '10:00-23:00', 'OPEN', 800, 4.5, NOW(), NOW()),
(3, '川味小馆', 'https://example.com/logo3.jpg', '正宗川菜，麻辣鲜香', '男生宿舍区', 39.9044, 116.4076, '13800138003', '11:00-22:30', 'OPEN', 600, 4.7, NOW(), NOW()),
(4, '甜心奶茶店', 'https://example.com/logo4.jpg', '各种奶茶和甜点，甜蜜时光', '女生宿舍区', 39.9045, 116.4077, '13800138004', '09:30-23:30', 'OPEN', 1500, 4.6, NOW(), NOW()),
(5, '健康轻食', 'https://example.com/logo5.jpg', '健康沙拉和三明治，营养均衡', '教学楼附近', 39.9046, 116.4078, '13800138005', '08:00-21:00', 'OPEN', 400, 4.9, NOW(), NOW());

-- 插入分类数据
INSERT INTO categories (merchant_id, name, sort, created_at, updated_at)
VALUES
(1, '主食', 1, NOW(), NOW()),
(1, '小吃', 2, NOW(), NOW()),
(1, '饮料', 3, NOW(), NOW()),
(2, '汉堡', 1, NOW(), NOW()),
(2, '薯条', 2, NOW(), NOW()),
(2, '饮料', 3, NOW(), NOW()),
(3, '川菜', 1, NOW(), NOW()),
(3, '小吃', 2, NOW(), NOW()),
(3, '汤品', 3, NOW(), NOW()),
(4, '奶茶', 1, NOW(), NOW()),
(4, '果茶', 2, NOW(), NOW()),
(4, '甜点', 3, NOW(), NOW()),
(5, '沙拉', 1, NOW(), NOW()),
(5, '三明治', 2, NOW(), NOW()),
(5, '果汁', 3, NOW(), NOW());

-- 插入菜品数据
-- 校园美食广场
INSERT INTO dishes (merchant_id, category_id, name, description, image, price, stock, sales, status, created_at, updated_at)
VALUES
(1, 1, '宫保鸡丁', '经典川菜，鸡肉嫩滑，花生香脆', 'https://example.com/dish1.jpg', 18.00, 100, 200, 'AVAILABLE', NOW(), NOW()),
(1, 1, '鱼香肉丝', '酸甜可口，口感丰富', 'https://example.com/dish2.jpg', 16.00, 100, 180, 'AVAILABLE', NOW(), NOW()),
(1, 2, '炸鸡腿', '外酥里嫩，香气扑鼻', 'https://example.com/dish3.jpg', 8.00, 150, 250, 'AVAILABLE', NOW(), NOW()),
(1, 2, '烤肠', '香辣可口，回味无穷', 'https://example.com/dish4.jpg', 4.00, 200, 300, 'AVAILABLE', NOW(), NOW()),
(1, 3, '可乐', '冰镇可乐，清爽解渴', 'https://example.com/dish5.jpg', 3.00, 300, 400, 'AVAILABLE', NOW(), NOW()),
(1, 3, '雪碧', '柠檬味汽水，清凉解暑', 'https://example.com/dish6.jpg', 3.00, 300, 350, 'AVAILABLE', NOW(), NOW());

-- 快乐汉堡
INSERT INTO dishes (merchant_id, category_id, name, description, image, price, stock, sales, status, created_at, updated_at)
VALUES
(2, 4, '经典汉堡', '牛肉饼，生菜，番茄，芝士', 'https://example.com/dish7.jpg', 15.00, 100, 220, 'AVAILABLE', NOW(), NOW()),
(2, 4, '双层汉堡', '两层牛肉饼，更多满足', 'https://example.com/dish8.jpg', 20.00, 80, 150, 'AVAILABLE', NOW(), NOW()),
(2, 5, '薯条', '金黄酥脆，口感绝佳', 'https://example.com/dish9.jpg', 8.00, 200, 300, 'AVAILABLE', NOW(), NOW()),
(2, 5, '芝士薯条', '薯条加芝士，口感丰富', 'https://example.com/dish10.jpg', 10.00, 150, 200, 'AVAILABLE', NOW(), NOW()),
(2, 6, '可乐', '冰镇可乐，搭配汉堡', 'https://example.com/dish11.jpg', 5.00, 300, 250, 'AVAILABLE', NOW(), NOW()),
(2, 6, '奶昔', '香草味奶昔，香甜可口', 'https://example.com/dish12.jpg', 12.00, 100, 150, 'AVAILABLE', NOW(), NOW());

-- 川味小馆
INSERT INTO dishes (merchant_id, category_id, name, description, image, price, stock, sales, status, created_at, updated_at)
VALUES
(3, 7, '麻婆豆腐', '麻辣鲜香，豆腐嫩滑', 'https://example.com/dish13.jpg', 16.00, 100, 180, 'AVAILABLE', NOW(), NOW()),
(3, 7, '水煮鱼', '鲜嫩多汁，麻辣可口', 'https://example.com/dish14.jpg', 38.00, 50, 100, 'AVAILABLE', NOW(), NOW()),
(3, 8, '担担面', '传统川菜，麻辣鲜香', 'https://example.com/dish15.jpg', 12.00, 150, 200, 'AVAILABLE', NOW(), NOW()),
(3, 8, '红油抄手', '皮薄馅大，麻辣鲜香', 'https://example.com/dish16.jpg', 14.00, 120, 150, 'AVAILABLE', NOW(), NOW()),
(3, 9, '酸辣汤', '酸辣开胃，营养丰富', 'https://example.com/dish17.jpg', 10.00, 100, 120, 'AVAILABLE', NOW(), NOW()),
(3, 9, '番茄蛋汤', '酸甜可口，营养开胃', 'https://example.com/dish18.jpg', 8.00, 100, 100, 'AVAILABLE', NOW(), NOW());

-- 甜心奶茶店
INSERT INTO dishes (merchant_id, category_id, name, description, image, price, stock, sales, status, created_at, updated_at)
VALUES
(4, 10, '珍珠奶茶', '经典珍珠奶茶，香甜可口', 'https://example.com/dish19.jpg', 12.00, 200, 300, 'AVAILABLE', NOW(), NOW()),
(4, 10, '波霸奶茶', '大颗波霸，口感丰富', 'https://example.com/dish20.jpg', 14.00, 180, 250, 'AVAILABLE', NOW(), NOW()),
(4, 11, '柠檬茶', '清新柠檬味，解暑解渴', 'https://example.com/dish21.jpg', 10.00, 250, 350, 'AVAILABLE', NOW(), NOW()),
(4, 11, '芒果茶', '新鲜芒果，香甜可口', 'https://example.com/dish22.jpg', 15.00, 200, 280, 'AVAILABLE', NOW(), NOW()),
(4, 12, '提拉米苏', '经典甜品，口感细腻', 'https://example.com/dish23.jpg', 18.00, 100, 150, 'AVAILABLE', NOW(), NOW()),
(4, 12, '芒果班戟', '新鲜芒果，香甜软糯', 'https://example.com/dish24.jpg', 16.00, 120, 180, 'AVAILABLE', NOW(), NOW());

-- 健康轻食
INSERT INTO dishes (merchant_id, category_id, name, description, image, price, stock, sales, status, created_at, updated_at)
VALUES
(5, 13, '凯撒沙拉', '新鲜蔬菜，凯撒酱', 'https://example.com/dish25.jpg', 22.00, 100, 120, 'AVAILABLE', NOW(), NOW()),
(5, 13, '水果沙拉', '新鲜水果，蜂蜜酸奶', 'https://example.com/dish26.jpg', 20.00, 120, 150, 'AVAILABLE', NOW(), NOW()),
(5, 14, '鸡肉三明治', '鸡胸肉，生菜，番茄', 'https://example.com/dish27.jpg', 18.00, 100, 180, 'AVAILABLE', NOW(), NOW()),
(5, 14, '鸡蛋三明治', '鸡蛋，生菜，芝士', 'https://example.com/dish28.jpg', 15.00, 120, 200, 'AVAILABLE', NOW(), NOW()),
(5, 15, '鲜榨橙汁', '新鲜橙子，营养丰富', 'https://example.com/dish29.jpg', 12.00, 150, 250, 'AVAILABLE', NOW(), NOW()),
(5, 15, '苹果汁', '新鲜苹果，清甜可口', 'https://example.com/dish30.jpg', 10.00, 150, 220, 'AVAILABLE', NOW(), NOW());