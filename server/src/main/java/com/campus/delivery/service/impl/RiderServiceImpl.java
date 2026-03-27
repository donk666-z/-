package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.dto.RiderProfileVO;
import com.campus.delivery.dto.RiderTaskVO;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Order;
import com.campus.delivery.entity.Rider;
import com.campus.delivery.entity.User;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.AddressMapper;
import com.campus.delivery.mapper.MerchantMapper;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RiderServiceImpl implements RiderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderRealtimeNotifier orderRealtimeNotifier;

    @Override
    public List<RiderTaskVO> getAvailableOrders(Long riderId) {
        Rider rider = ensureRider(riderId);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, Arrays.asList("accepted", "prepared"))
                .isNull(Order::getRiderId)
                .orderByAsc(Order::getCreatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(order -> buildTaskVO(order, rider))
                .collect(Collectors.toList());
    }

    @Override
    public RiderTaskVO getCurrentTask(Long riderId) {
        Rider rider = ensureRider(riderId);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getRiderId, riderId)
                .in(Order::getStatus, Arrays.asList("accepted", "prepared", "delivering"))
                .orderByDesc(Order::getUpdatedAt)
                .orderByDesc(Order::getCreatedAt)
                .last("limit 1");
        Order order = orderMapper.selectOne(wrapper);
        return order == null ? null : buildTaskVO(order, rider);
    }

    @Override
    public RiderTaskVO getTaskDetail(Long orderId, Long riderId) {
        Rider rider = ensureRider(riderId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getRiderId() == null || !riderId.equals(order.getRiderId())) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return buildTaskVO(order, rider);
    }

    @Override
    public List<RiderTaskVO> getHistoryOrders(Long riderId) {
        Rider rider = ensureRider(riderId);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getRiderId, riderId)
                .eq(Order::getStatus, "completed")
                .orderByDesc(Order::getCompletedTime)
                .orderByDesc(Order::getUpdatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(order -> buildTaskVO(order, rider))
                .collect(Collectors.toList());
    }

    @Override
    public void grabOrder(Long orderId, Long riderId) {
        ensureRider(riderId);

        LambdaQueryWrapper<Order> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(Order::getRiderId, riderId)
                .in(Order::getStatus, Arrays.asList("accepted", "prepared", "delivering"));
        Long activeCount = orderMapper.selectCount(activeWrapper);
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(400, "请先完成当前订单");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getRiderId() != null) {
            throw new BusinessException(400, "订单已被其他骑手接单");
        }
        if (!Arrays.asList("accepted", "prepared").contains(order.getStatus())) {
            throw new BusinessException(400, "当前订单暂不可抢");
        }

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

    private RiderTaskVO buildTaskVO(Order order, Rider rider) {
        Merchant merchant = order.getMerchantId() == null ? null : merchantMapper.selectById(order.getMerchantId());
        User user = order.getUserId() == null ? null : userMapper.selectById(order.getUserId());
        Address address = resolveAddress(order.getUserId(), order.getAddress());

        RiderTaskVO vo = new RiderTaskVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setMerchantId(order.getMerchantId());
        vo.setRiderId(order.getRiderId());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setRemark(order.getRemark());
        vo.setStatus(order.getStatus());
        vo.setEstimatedTime(formatEstimated(order.getEstimatedTime()));
        vo.setCreatedAt(order.getCreatedAt());
        vo.setCompletedTime(order.getCompletedTime());
        vo.setDeliveryAddress(order.getAddress());
        vo.setCustomerPhone(StringUtils.hasText(order.getPhone()) ? order.getPhone() : (address != null ? address.getPhone() : null));

        if (merchant != null) {
            vo.setMerchantName(merchant.getName());
            vo.setMerchantAddress(merchant.getAddress());
            vo.setMerchantLatitude(toDouble(merchant.getLatitude()));
            vo.setMerchantLongitude(toDouble(merchant.getLongitude()));
        }

        if (address != null) {
            vo.setCustomerName(address.getName());
            vo.setDeliveryLatitude(toDouble(address.getLatitude()));
            vo.setDeliveryLongitude(toDouble(address.getLongitude()));
        } else if (user != null) {
            vo.setCustomerName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getPhone());
        }

        if (!StringUtils.hasText(vo.getCustomerName()) && rider != null) {
            vo.setCustomerName("用户" + order.getUserId());
        }

        return vo;
    }

    private Address resolveAddress(Long userId, String orderAddressLine) {
        if (userId == null || !StringUtils.hasText(orderAddressLine)) {
            return null;
        }

        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt);

        String normalizedOrderAddress = normalizeAddress(orderAddressLine);
        for (Address address : addressMapper.selectList(wrapper)) {
            if (normalizedOrderAddress.equals(normalizeAddress(buildAddressLine(address)))) {
                return address;
            }
        }
        return null;
    }

    private String buildAddressLine(Address address) {
        return (nullToEmpty(address.getAddress()) + " "
                + nullToEmpty(address.getBuilding()) + " "
                + nullToEmpty(address.getRoom())).trim();
    }

    private String normalizeAddress(String value) {
        return nullToEmpty(value).replaceAll("\\s+", " ").trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String formatEstimated(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return null;
        }
        return "约 " + minutes + " 分钟";
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
