-- 校园外卖配送系统 - 更新图片URL

USE campus_delivery;

-- 更新商户Logo
UPDATE merchants SET logo = 'https://images.pexels.com/photos/2641886/pexels-photo-2641886.jpeg?auto=compress&cs=tinysrgb&w=200' WHERE id = 1;
UPDATE merchants SET logo = 'https://images.pexels.com/photos/1126359/pexels-photo-1126359.jpeg?auto=compress&cs=tinysrgb&w=200' WHERE id = 2;
UPDATE merchants SET logo = 'https://images.pexels.com/photos/2092897/pexels-photo-2092897.jpeg?auto=compress&cs=tinysrgb&w=200' WHERE id = 3;
UPDATE merchants SET logo = 'https://images.pexels.com/photos/1352274/pexels-photo-1352274.jpeg?auto=compress&cs=tinysrgb&w=200' WHERE id = 4;
UPDATE merchants SET logo = 'https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=200' WHERE id = 5;

-- 校园美食广场的菜品
UPDATE dishes SET image = 'https://images.pexels.com/photos/2097090/pexels-photo-2097090.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 1 AND name = '宫保鸡丁';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 1 AND name = '鱼香肉丝';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1893556/pexels-photo-1893556.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 1 AND name = '炸鸡腿';
UPDATE dishes SET image = 'https://images.pexels.com/photos/106343/pexels-photo-106343.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 1 AND name = '烤肠';
UPDATE dishes SET image = 'https://images.pexels.com/photos/305765/pexels-photo-305765.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 1 AND name = '可乐';
UPDATE dishes SET image = 'https://images.pexels.com/photos/96974/pexels-photo-96974.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 1 AND name = '雪碧';

-- 快乐汉堡的菜品
UPDATE dishes SET image = 'https://images.pexels.com/photos/1126359/pexels-photo-1126359.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 2 AND name = '经典汉堡';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1251198/pexels-photo-1251198.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 2 AND name = '双层汉堡';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1583385/pexels-photo-1583385.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 2 AND name = '薯条';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1352255/pexels-photo-1352255.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 2 AND name = '芝士薯条';
UPDATE dishes SET image = 'https://images.pexels.com/photos/305765/pexels-photo-305765.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 2 AND name = '可乐';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1352251/pexels-photo-1352251.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 2 AND name = '奶昔';

-- 川味小馆的菜品
UPDATE dishes SET image = 'https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 3 AND name = '麻婆豆腐';
UPDATE dishes SET image = 'https://images.pexels.com/photos/2051338/pexels-photo-2051338.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 3 AND name = '水煮鱼';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1365425/pexels-photo-1365425.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 3 AND name = '担担面';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1365425/pexels-photo-1365425.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 3 AND name = '红油抄手';
UPDATE dishes SET image = 'https://images.pexels.com/photos/2097090/pexels-photo-2097090.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 3 AND name = '酸辣汤';
UPDATE dishes SET image = 'https://images.pexels.com/photos/2097090/pexels-photo-2097090.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 3 AND name = '番茄蛋汤';

-- 甜心奶茶店的菜品
UPDATE dishes SET image = 'https://images.pexels.com/photos/1352274/pexels-photo-1352274.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 4 AND name = '珍珠奶茶';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1352274/pexels-photo-1352274.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 4 AND name = '波霸奶茶';
UPDATE dishes SET image = 'https://images.pexels.com/photos/96974/pexels-photo-96974.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 4 AND name = '柠檬茶';
UPDATE dishes SET image = 'https://images.pexels.com/photos/96974/pexels-photo-96974.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 4 AND name = '芒果茶';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1126359/pexels-photo-1126359.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 4 AND name = '提拉米苏';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1126359/pexels-photo-1126359.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 4 AND name = '芒果班戟';

-- 健康轻食的菜品
UPDATE dishes SET image = 'https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 5 AND name = '凯撒沙拉';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 5 AND name = '水果沙拉';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1251198/pexels-photo-1251198.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 5 AND name = '鸡肉三明治';
UPDATE dishes SET image = 'https://images.pexels.com/photos/1251198/pexels-photo-1251198.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 5 AND name = '鸡蛋三明治';
UPDATE dishes SET image = 'https://images.pexels.com/photos/96974/pexels-photo-96974.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 5 AND name = '鲜榨橙汁';
UPDATE dishes SET image = 'https://images.pexels.com/photos/96974/pexels-photo-96974.jpeg?auto=compress&cs=tinysrgb&w=400' WHERE merchant_id = 5 AND name = '苹果汁';