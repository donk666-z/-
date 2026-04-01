package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Order;
import com.campus.delivery.entity.Transaction;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.mapper.OrderMapper;
import com.campus.delivery.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
@RequestMapping("/admin/finance")
public class AdminFinanceController {

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private OrderMapper orderMapper;

    @GetMapping("/transactions")
    public Result<Page<Map<String, Object>>> listTransactions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<>();
        if (merchantId != null) {
            wrapper.eq(Transaction::getMerchantId, merchantId);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Transaction::getType, type.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Transaction::getStatus, status.trim());
        }
        LocalDateTime start = parseStartTime(startTime);
        LocalDateTime end = parseEndTime(endTime);
        if (start != null) {
            wrapper.ge(Transaction::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.le(Transaction::getCreatedAt, end);
        }
        wrapper.orderByDesc(Transaction::getCreatedAt);

        Page<Transaction> sourcePage = transactionMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> records = toFinanceRecords(sourcePage.getRecords());

        Page<Map<String, Object>> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        targetPage.setRecords(records);
        return Result.success(targetPage);
    }

    @PutMapping("/transactions/{id}/status")
    public Result<Void> updateTransactionStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body == null ? null : body.get("status");
        if (!StringUtils.hasText(status)) {
            return Result.error(400, "状态不能为空");
        }
        Transaction tx = transactionMapper.selectById(id);
        if (tx == null) {
            return Result.error(404, "流水不存在");
        }
        tx.setStatus(status.trim());
        tx.setUpdatedAt(LocalDateTime.now());
        transactionMapper.updateById(tx);
        return Result.success("状态更新成功", null);
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        List<Transaction> all = transactionMapper.selectList(new LambdaQueryWrapper<Transaction>().orderByDesc(Transaction::getCreatedAt));

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal completedAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        int pendingCount = 0;
        int completedCount = 0;
        int failedCount = 0;
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        BigDecimal todayAmount = BigDecimal.ZERO;

        for (Transaction tx : all) {
            BigDecimal amount = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
            totalAmount = totalAmount.add(amount);
            if ("completed".equalsIgnoreCase(tx.getStatus())) {
                completedAmount = completedAmount.add(amount);
                completedCount++;
            } else if ("pending".equalsIgnoreCase(tx.getStatus())) {
                pendingAmount = pendingAmount.add(amount);
                pendingCount++;
            } else if ("failed".equalsIgnoreCase(tx.getStatus())) {
                failedCount++;
            }
            if (tx.getCreatedAt() != null && !tx.getCreatedAt().isBefore(todayStart)) {
                todayAmount = todayAmount.add(amount);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalAmount", totalAmount);
        data.put("completedAmount", completedAmount);
        data.put("pendingAmount", pendingAmount);
        data.put("todayAmount", todayAmount);
        data.put("pendingCount", pendingCount);
        data.put("completedCount", completedCount);
        data.put("failedCount", failedCount);
        data.put("totalCount", all.size());
        return Result.success(data);
    }

    private List<Map<String, Object>> toFinanceRecords(List<Transaction> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> merchantIds = records.stream()
                .map(Transaction::getMerchantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> orderIds = records.stream()
                .map(Transaction::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> merchantNameMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            List<Merchant> merchants = merchantMapper.selectBatchIds(merchantIds);
            merchantNameMap = merchants.stream().collect(Collectors.toMap(Merchant::getId, Merchant::getName));
        }

        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<Order> orders = orderMapper.selectBatchIds(orderIds);
            orderNoMap = orders.stream().collect(Collectors.toMap(Order::getId, Order::getOrderNo));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Transaction tx : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tx.getId());
            item.put("merchantId", tx.getMerchantId());
            item.put("merchantName", merchantNameMap.getOrDefault(tx.getMerchantId(), ""));
            item.put("orderId", tx.getOrderId());
            item.put("orderNo", orderNoMap.getOrDefault(tx.getOrderId(), ""));
            item.put("amount", tx.getAmount());
            item.put("type", tx.getType());
            item.put("status", tx.getStatus());
            item.put("remark", tx.getRemark());
            item.put("createdAt", tx.getCreatedAt());
            item.put("updatedAt", tx.getUpdatedAt());
            list.add(item);
        }
        return list;
    }

    private LocalDateTime parseStartTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value.trim()).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private LocalDateTime parseEndTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().minusSeconds(1);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
