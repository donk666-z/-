package com.campus.delivery.service;

import com.campus.delivery.entity.Order;

public interface OrderRealtimeNotifier {

    void notifyOrderStatus(Long orderId, String status, String estimatedTimeText);

    void notifyRiderLocation(Long orderId, double lat, double lng);

    void notifyMerchantNewOrder(Order order);
}
