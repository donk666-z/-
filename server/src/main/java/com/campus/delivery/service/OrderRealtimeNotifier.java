package com.campus.delivery.service;

import com.campus.delivery.entity.Order;

public interface OrderRealtimeNotifier {

    void notifyOrderStatus(Long orderId, String status, String estimatedTimeText);

    void notifyRiderLocation(Long orderId, double lat, double lng);

    void notifyMerchantNewOrder(Order order);

    void notifyRiderOrdersUpdated(String reason);

    void notifyRiderAssigned(Long riderId, Long orderId, String message);
}
