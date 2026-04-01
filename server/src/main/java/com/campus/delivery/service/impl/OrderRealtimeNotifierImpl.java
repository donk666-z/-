package com.campus.delivery.service.impl;

import com.campus.delivery.entity.Order;
import com.campus.delivery.service.OrderRealtimeNotifier;
import com.campus.delivery.websocket.OrderTrackingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderRealtimeNotifierImpl implements OrderRealtimeNotifier {

    @Autowired
    private OrderTrackingWebSocketHandler orderTrackingWebSocketHandler;

    @Override
    public void notifyOrderStatus(Long orderId, String status, String estimatedTimeText) {
        orderTrackingWebSocketHandler.broadcastOrderStatus(orderId, status, estimatedTimeText);
    }

    @Override
    public void notifyRiderLocation(Long orderId, double lat, double lng) {
        orderTrackingWebSocketHandler.broadcastRiderLocation(orderId, lat, lng);
    }

    @Override
    public void notifyMerchantNewOrder(Order order) {
        orderTrackingWebSocketHandler.broadcastMerchantNewOrder(order);
    }

    @Override
    public void notifyRiderOrdersUpdated(String reason) {
        orderTrackingWebSocketHandler.broadcastRiderOrdersUpdated(reason);
    }

    @Override
    public void notifyRiderAssigned(Long riderId, Long orderId, String message) {
        orderTrackingWebSocketHandler.broadcastRiderAssigned(riderId, orderId, message);
    }
}
