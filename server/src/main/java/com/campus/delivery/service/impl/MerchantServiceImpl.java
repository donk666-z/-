package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Order;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.mapper.OrderMapper;
import com.campus.delivery.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public List<Merchant> getList(String keyword, String status) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Merchant::getName, keyword);
        }
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Merchant::getStatus, status);
        }

        List<Merchant> merchants = list(wrapper);
        fillMonthSales(merchants);
        return merchants;
    }

    @Override
    public Merchant getDetail(Long id) {
        Merchant merchant = getById(id);
        fillMonthSales(merchant);
        return merchant;
    }

    @Override
    public Merchant getByUserId(Long userId) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, userId);
        return getOne(wrapper);
    }

    private void fillMonthSales(Merchant merchant) {
        if (merchant == null || merchant.getId() == null) {
            return;
        }
        Map<Long, Integer> monthSalesMap = queryMonthSalesMap(Collections.singleton(merchant.getId()));
        merchant.setMonthSales(monthSalesMap.getOrDefault(merchant.getId(), 0));
    }

    private void fillMonthSales(List<Merchant> merchants) {
        if (merchants == null || merchants.isEmpty()) {
            return;
        }
        Set<Long> merchantIds = merchants.stream()
                .map(Merchant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (merchantIds.isEmpty()) {
            return;
        }
        Map<Long, Integer> monthSalesMap = queryMonthSalesMap(merchantIds);
        merchants.forEach(merchant -> merchant.setMonthSales(monthSalesMap.getOrDefault(merchant.getId(), 0)));
    }

    private Map<Long, Integer> queryMonthSalesMap(Set<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getMerchantId, merchantIds)
                .eq(Order::getStatus, "completed")
                .ge(Order::getCreatedAt, LocalDateTime.now().minusDays(30));

        Map<Long, Integer> result = new HashMap<>();
        for (Order order : orderMapper.selectList(wrapper)) {
            if (order == null || order.getMerchantId() == null) {
                continue;
            }
            result.put(order.getMerchantId(), result.getOrDefault(order.getMerchantId(), 0) + 1);
        }
        return result;
    }
}
