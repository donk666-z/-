package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.dto.RiderProfileVO;
import com.campus.delivery.entity.Order;
import com.campus.delivery.entity.Rider;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.OrderMapper;
import com.campus.delivery.mapper.RiderMapper;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.OrderRealtimeNotifier;
import com.campus.delivery.service.RiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class RiderServiceImpl implements RiderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderRealtimeNotifier orderRealtimeNotifier;

    @Override
    public List<Order> getAvailableOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "prepared")
               .isNull(Order::getRiderId)
               .orderByAsc(Order::getCreatedAt);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public void grabOrder(Long orderId, Long riderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getRiderId() != null) {
            throw new RuntimeException("订单已被抢");
        }

        ensureRider(riderId);
        order.setRiderId(riderId);
        orderMapper.updateById(order);
    }

    @Override
    public void updateLocation(Long riderId, Double latitude, Double longitude, Long orderId) {
        ensureRider(riderId);

        Rider rider = new Rider();
        rider.setId(riderId);
        rider.setLatitude(new BigDecimal(latitude));
        rider.setLongitude(new BigDecimal(longitude));
        riderMapper.updateById(rider);

        if (orderId != null) {
            Map<String, Double> location = new HashMap<>();
            location.put("latitude", latitude);
            location.put("longitude", longitude);
            redisTemplate.opsForValue().set("rider:location:" + orderId, location, 5, TimeUnit.MINUTES);
            orderRealtimeNotifier.notifyRiderLocation(orderId, latitude, longitude);
        }
    }

    @Override
    public Map<String, Object> getStats(Long riderId) {
        Rider rider = ensureRider(riderId);

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getRiderId, riderId)
               .eq(Order::getStatus, "completed")
               .ge(Order::getCompletedTime, todayStart);
        List<Order> todayCompletedOrders = orderMapper.selectList(wrapper);

        long todayOrders = todayCompletedOrders.size();
        BigDecimal todayEarnings = todayCompletedOrders.stream()
                .map(Order::getDeliveryFee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalOrders = rider.getTotalOrders() == null ? 0 : rider.getTotalOrders();
        BigDecimal totalIncome = rider.getTotalIncome() == null ? BigDecimal.ZERO : rider.getTotalIncome();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayOrders", todayOrders);
        stats.put("totalOrders", totalOrders);
        stats.put("totalIncome", totalIncome);
        stats.put("todayDeliveries", todayOrders);
        stats.put("totalDeliveries", totalOrders);
        stats.put("todayEarnings", todayEarnings);
        stats.put("totalEarnings", totalIncome);
        return stats;
    }

    @Override
    public RiderProfileVO getProfile(Long riderId) {
        Rider rider = ensureRider(riderId);
        RiderProfileVO profile = new RiderProfileVO();
        profile.setId(rider.getId());
        profile.setName(rider.getName());
        profile.setPhone(rider.getPhone());
        profile.setAvatar(rider.getAvatar());
        profile.setStatus(rider.getStatus());
        profile.setTotalOrders(rider.getTotalOrders() == null ? 0 : rider.getTotalOrders());
        profile.setTotalIncome(rider.getTotalIncome() == null ? BigDecimal.ZERO : rider.getTotalIncome());
        profile.setLatitude(rider.getLatitude());
        profile.setLongitude(rider.getLongitude());
        return profile;
    }

    @Override
    public void updateStatus(Long riderId, String status) {
        String normalizedStatus = normalizeStatus(status);
        ensureRider(riderId);

        Rider rider = new Rider();
        rider.setId(riderId);
        rider.setStatus(normalizedStatus);
        riderMapper.updateById(rider);
    }

    private Rider ensureRider(Long riderId) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider != null) {
            return rider;
        }

        User user = userMapper.selectById(riderId);
        if (user == null || !"rider".equals(user.getRole())) {
            throw new RuntimeException("骑手不存在");
        }

        Rider newRider = new Rider();
        newRider.setId(user.getId());
        newRider.setOpenid(user.getOpenid());
        newRider.setName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "骑手" + user.getId());
        newRider.setPhone(user.getPhone());
        newRider.setAvatar(user.getAvatar());
        newRider.setStatus("offline");
        newRider.setTotalOrders(0);
        newRider.setTotalIncome(BigDecimal.ZERO);
        riderMapper.insert(newRider);
        return riderMapper.selectById(riderId);
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toLowerCase();
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("骑手状态不能为空");
        }
        if (!"online".equals(value) && !"offline".equals(value) && !"delivering".equals(value)) {
            throw new RuntimeException("骑手状态不合法");
        }
        return value;
    }
}
