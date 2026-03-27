package com.campus.delivery.service;

public interface OrderRealtimeNotifier {

    void notifyOrderStatus(Long orderId, String status, String estimatedTimeText);

    void notifyRiderLocation(Long orderId, double lat, double lng);
}
