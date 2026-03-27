package com.campus.delivery.service;

import com.campus.delivery.dto.RiderProfileVO;
import com.campus.delivery.entity.Order;

import java.util.List;
import java.util.Map;

public interface RiderService {

    List<Order> getAvailableOrders();

    void grabOrder(Long orderId, Long riderId);

    void updateLocation(Long riderId, Double latitude, Double longitude, Long orderId);

    Map<String, Object> getStats(Long riderId);

    RiderProfileVO getProfile(Long riderId);

    void updateStatus(Long riderId, String status);
}
