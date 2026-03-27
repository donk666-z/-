package com.campus.delivery.service;

import com.campus.delivery.dto.RiderProfileVO;
import com.campus.delivery.dto.RiderTaskVO;

import java.util.List;
import java.util.Map;

public interface RiderService {

    List<RiderTaskVO> getAvailableOrders(Long riderId);

    RiderTaskVO getCurrentTask(Long riderId);

    RiderTaskVO getTaskDetail(Long orderId, Long riderId);

    List<RiderTaskVO> getHistoryOrders(Long riderId);

    void grabOrder(Long orderId, Long riderId);

    void updateLocation(Long riderId, Double latitude, Double longitude, Long orderId);

    Map<String, Object> getStats(Long riderId);

    RiderProfileVO getProfile(Long riderId);

    void updateStatus(Long riderId, String status);
}
