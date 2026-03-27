package com.campus.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.delivery.dto.CreateOrderDTO;
import com.campus.delivery.dto.StudentOrderVO;
import com.campus.delivery.entity.Order;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<Order> {

    Order createOrder(CreateOrderDTO dto, Long userId);

    List<StudentOrderVO> getStudentOrderList(Long userId, String status);

    StudentOrderVO getStudentOrderDetail(Long id, Long userId);

    List<StudentOrderVO> getMerchantOrderList(Long merchantId, String status);

    StudentOrderVO getMerchantOrderDetail(Long id, Long merchantId);

    Order getOrderDetail(Long id);

    void cancelOrder(Long id);

    void studentCancelOrder(Long id, Long userId);

    void studentConfirmReceipt(Long id, Long userId);

    void acceptOrder(Long id);

    void prepareOrder(Long id);

    void pickupOrder(Long id, Long riderId);

    void deliverOrder(Long id);

    List<Order> getOrdersByMerchant(Long merchantId, String status);

    List<Order> getOrdersByRider(Long riderId, String status);

    Map<String, Object> getMerchantStats(Long merchantId);

    Map<String, Object> getPlatformStats();
}
