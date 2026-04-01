package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Order;
import com.campus.delivery.entity.Rider;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.mapper.RiderMapper;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private RiderMapper riderMapper;

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> getList(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String orderNo,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status.trim());
        }
        if (StringUtils.hasText(orderNo)) {
            wrapper.like(Order::getOrderNo, orderNo.trim());
        }
        LocalDateTime start = parseDateStart(startDate);
        LocalDateTime end = parseDateEnd(endDate);
        if (start != null) {
            wrapper.ge(Order::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.le(Order::getCreatedAt, end);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> sourcePage = orderService.page(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        targetPage.setRecords(toRecords(sourcePage.getRecords()));
        return Result.success(targetPage);
    }

    @GetMapping("/{id}")
    public Result<Order> getDetail(@PathVariable Long id) {
        Order order = orderService.getOrderDetail(id);
        return Result.success(order);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success("订单已取消", null);
    }

    private List<Map<String, Object>> toRecords(List<Order> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = records.stream().map(Order::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> merchantIds = records.stream().map(Order::getMerchantId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> riderIds = records.stream().map(Order::getRiderId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> userMap = new HashMap<>();
        Map<Long, Merchant> merchantMap = new HashMap<>();
        Map<Long, Rider> riderMap = new HashMap<>();

        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, item -> item));
        }
        if (!merchantIds.isEmpty()) {
            merchantMap = merchantMapper.selectBatchIds(merchantIds).stream().collect(Collectors.toMap(Merchant::getId, item -> item));
        }
        if (!riderIds.isEmpty()) {
            riderMap = riderMapper.selectBatchIds(riderIds).stream().collect(Collectors.toMap(Rider::getId, item -> item));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Order order : records) {
            User user = userMap.get(order.getUserId());
            Merchant merchant = merchantMap.get(order.getMerchantId());
            Rider rider = riderMap.get(order.getRiderId());

            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("status", order.getStatus());
            item.put("dishPrice", order.getDishPrice());
            item.put("deliveryFee", order.getDeliveryFee());
            item.put("totalPrice", order.getTotalPrice());
            item.put("address", order.getAddress());
            item.put("phone", order.getPhone());
            item.put("remark", order.getRemark());
            item.put("paymentMethod", order.getPaymentMethod());
            item.put("paymentTime", order.getPaymentTime());
            item.put("completedTime", order.getCompletedTime());
            item.put("createdAt", order.getCreatedAt());
            item.put("updatedAt", order.getUpdatedAt());
            item.put("userId", order.getUserId());
            item.put("userName", user == null ? "" : user.getNickname());
            item.put("userPhone", user == null ? "" : user.getPhone());
            item.put("merchantId", order.getMerchantId());
            item.put("merchantName", merchant == null ? "" : merchant.getName());
            item.put("riderId", order.getRiderId());
            item.put("riderName", rider == null ? "" : rider.getName());
            item.put("riderPhone", rider == null ? "" : rider.getPhone());
            list.add(item);
        }
        return list;
    }

    private LocalDateTime parseDateStart(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atStartOfDay();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateEnd(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().minusSeconds(1);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
