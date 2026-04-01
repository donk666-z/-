package com.campus.delivery.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.dto.RoutePointVO;
import com.campus.delivery.dto.RouteStepVO;
import com.campus.delivery.dto.RiderProfileVO;
import com.campus.delivery.dto.RiderRouteVO;
import com.campus.delivery.dto.RiderTaskVO;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Order;
import com.campus.delivery.entity.Rider;
import com.campus.delivery.entity.SystemConfig;
import com.campus.delivery.entity.User;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.AddressMapper;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.mapper.OrderMapper;
import com.campus.delivery.mapper.RiderMapper;
import com.campus.delivery.mapper.SystemConfigMapper;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.OrderRealtimeNotifier;
import com.campus.delivery.service.RiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RiderServiceImpl implements RiderService {
    private static final Logger log = LoggerFactory.getLogger(RiderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderRealtimeNotifier orderRealtimeNotifier;

    @Value("${tencent.map.key:}")
    private String tencentMapKey;

    @Value("${tencent.map.route-mode:ebicycling}")
    private String defaultRouteMode;

    @Override
    public List<RiderTaskVO> getAvailableOrders(Long riderId) {
        Rider rider = ensureRider(riderId);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, Arrays.asList("accepted", "prepared"))
                .isNull(Order::getRiderId)
                .orderByAsc(Order::getCreatedAt);
        List<RiderTaskVO> tasks = orderMapper.selectList(wrapper).stream()
                .map(order -> buildTaskVO(order, rider))
                .collect(Collectors.toList());

        RoutePointVO riderPoint = buildPoint(toDouble(rider.getLatitude()), toDouble(rider.getLongitude()));
        int nearbyRadiusMeters = resolveNearbyRadiusMeters();
        if (riderPoint == null) {
            return tasks;
        }

        List<RiderTaskVO> nearbyTasks = new ArrayList<>();
        for (RiderTaskVO task : tasks) {
            RoutePointVO merchantPoint = buildPoint(task.getMerchantLatitude(), task.getMerchantLongitude());
            if (merchantPoint == null) {
                continue;
            }
            int distanceMeters = estimateDistance(riderPoint, merchantPoint);
            if (distanceMeters > nearbyRadiusMeters) {
                continue;
            }
            task.setDistance(roundDistanceKm(distanceMeters));
            nearbyTasks.add(task);
        }
        nearbyTasks.sort((a, b) -> Double.compare(
                a.getDistance() == null ? Double.MAX_VALUE : a.getDistance(),
                b.getDistance() == null ? Double.MAX_VALUE : b.getDistance()
        ));
        return nearbyTasks;
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
            throw new BusinessException(404, "Order not found");
        }
        if (order.getRiderId() == null || !riderId.equals(order.getRiderId())) {
            throw new BusinessException(403, "No permission to access this task");
        }
        return buildTaskVO(order, rider);
    }

    @Override
    public RiderRouteVO getRoutePlan(Long orderId, Long riderId, Double fromLat, Double fromLng) {
        RiderTaskVO task = getTaskDetail(orderId, riderId);
        RoutePointVO destination = buildPoint(task.getDeliveryLatitude(), task.getDeliveryLongitude());
        RouteOrigin origin = resolveRouteOrigin(task, riderId, fromLat, fromLng);

        if (origin.point == null || destination == null) {
            return buildUnavailableRoute(task, origin);
        }

        String mapKey = resolveTencentMapKey();
        String fallbackNote = buildFallbackNote(StringUtils.hasText(mapKey), null);
        try {
            RiderRouteVO smartRoute = requestTencentRoute(origin, task, destination, mapKey);
            if (smartRoute != null) {
                return smartRoute;
            }
            fallbackNote = buildFallbackNote(StringUtils.hasText(mapKey), "腾讯算路未返回有效路线");
        } catch (Exception ex) {
            // Fall back to direct estimation when Tencent route planning is unavailable.
            log.warn("Tencent route planning failed for orderId={}, riderId={}: {}", orderId, riderId, ex.getMessage());
            fallbackNote = buildFallbackNote(StringUtils.hasText(mapKey), ex.getMessage());
        }

        return buildFallbackRoute(origin, task, destination, fallbackNote);
    }

    @Override
    public RiderRouteVO getStudentRoutePlan(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(403, "No permission to access this order");
        }

        RiderTaskVO task = buildTaskVO(order, null);
        RoutePointVO destination = buildPoint(task.getDeliveryLatitude(), task.getDeliveryLongitude());

        RouteOrigin origin = new RouteOrigin();
        origin.sourceType = "merchant";
        origin.label = StringUtils.hasText(task.getMerchantName()) ? task.getMerchantName() : "Merchant";

        if ("delivering".equals(order.getStatus())) {
            RoutePointVO realtimeRiderPoint = resolveRealtimeRiderPoint(orderId);
            if (realtimeRiderPoint != null) {
                origin.point = realtimeRiderPoint;
                origin.sourceType = "current";
                origin.label = "Current Location";
            } else if (order.getRiderId() != null) {
                Rider rider = riderMapper.selectById(order.getRiderId());
                RoutePointVO riderPoint = rider == null ? null : buildPoint(toDouble(rider.getLatitude()), toDouble(rider.getLongitude()));
                if (riderPoint != null) {
                    origin.point = riderPoint;
                    origin.sourceType = "current";
                    origin.label = "Current Location";
                }
            }
        }

        if (origin.point == null) {
            origin.point = buildPoint(task.getMerchantLatitude(), task.getMerchantLongitude());
        }

        if (origin.point == null || destination == null) {
            return buildUnavailableRoute(task, origin);
        }

        String mapKey = resolveTencentMapKey();
        String fallbackNote = buildFallbackNote(StringUtils.hasText(mapKey), null);
        try {
            RiderRouteVO smartRoute = requestTencentRoute(origin, task, destination, mapKey);
            if (smartRoute != null) {
                return smartRoute;
            }
            fallbackNote = buildFallbackNote(StringUtils.hasText(mapKey), "腾讯算路未返回有效路线");
        } catch (Exception ex) {
            log.warn("Tencent route planning failed for student orderId={}, userId={}: {}", orderId, userId, ex.getMessage());
            fallbackNote = buildFallbackNote(StringUtils.hasText(mapKey), ex.getMessage());
        }

        return buildFallbackRoute(origin, task, destination, fallbackNote);
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
            throw new BusinessException(400, "Please finish the current order first");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        if (order.getRiderId() != null) {
            throw new BusinessException(400, "Order has been taken by another rider");
        }
        if (!Arrays.asList("accepted", "prepared").contains(order.getStatus())) {
            throw new BusinessException(400, "Current order cannot be grabbed");
        }

        order.setRiderId(riderId);
        orderMapper.updateById(order);
        orderRealtimeNotifier.notifyRiderOrdersUpdated("grabbed");
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
            vo.setCustomerName("User" + order.getUserId());
        }

        return vo;
    }

    private RouteOrigin resolveRouteOrigin(RiderTaskVO task, Long riderId, Double fromLat, Double fromLng) {
        RouteOrigin origin = new RouteOrigin();
        origin.sourceType = "merchant";
        origin.label = StringUtils.hasText(task.getMerchantName()) ? task.getMerchantName() : "Merchant";

        RoutePointVO currentPoint = buildPoint(fromLat, fromLng);
        if (currentPoint != null) {
            origin.point = currentPoint;
            origin.sourceType = "current";
            origin.label = "Current Location";
            return origin;
        }

        if ("delivering".equals(task.getStatus())) {
            Rider rider = ensureRider(riderId);
            RoutePointVO riderPoint = buildPoint(toDouble(rider.getLatitude()), toDouble(rider.getLongitude()));
            if (riderPoint != null) {
                origin.point = riderPoint;
                origin.sourceType = "current";
                origin.label = "Current Location";
                return origin;
            }
        }

        origin.point = buildPoint(task.getMerchantLatitude(), task.getMerchantLongitude());
        return origin;
    }

    private RiderRouteVO requestTencentRoute(RouteOrigin origin, RiderTaskVO task, RoutePointVO destination, String mapKey) {
        if (!StringUtils.hasText(mapKey)) {
            return null;
        }

        String routeMode = normalizeRouteMode(resolveRouteMode());
        String url = "https://apis.map.qq.com/ws/direction/v1/" + routeMode + "/";
        String response = HttpUtil.createGet(url)
                .form("from", formatPoint(origin.point))
                .form("to", formatPoint(destination))
                .form("output", "json")
                .form("key", mapKey)
                .timeout(5000)
                .execute()
                .body();

        JSONObject resultObject = JSONUtil.parseObj(response);
        if (resultObject.getInt("status", -1) != 0) {
            throw new RuntimeException("腾讯路线接口返回异常: " + resultObject.getStr("message", "未知错误"));
        }

        JSONObject result = resultObject.getJSONObject("result");
        if (result == null) {
            return null;
        }

        JSONArray routes = result.getJSONArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("腾讯路线接口未返回 routes");
        }

        JSONObject route = routes.getJSONObject(0);
        RiderRouteVO routeVO = createRouteSkeleton(origin, task);
        routeVO.setAvailable(true);
        routeVO.setSmart(true);
        routeVO.setProvider("tencent");
        routeVO.setMode(normalizeRouteMode(route.getStr("mode")).toUpperCase());
        routeVO.setDistanceMeters(route.getInt("distance", estimateDistance(origin.point, destination)));
        routeVO.setDurationMinutes(normalizeDurationMinutes(route.getInt("duration"), routeVO.getDistanceMeters()));
        routeVO.setEtaText(buildEtaText(routeVO.getDurationMinutes()));
        routeVO.setNote("Tencent e-bike route planning enabled");
        routeVO.setPoints(buildRoutePoints(route, origin.point, destination));
        routeVO.setSteps(parseSteps(route.getJSONArray("steps")));

        if (routeVO.getPoints().isEmpty()) {
            routeVO.setPoints(Arrays.asList(origin.point, destination));
        }
        return routeVO;
    }

    private RiderRouteVO buildFallbackRoute(RouteOrigin origin, RiderTaskVO task, RoutePointVO destination, String note) {
        RiderRouteVO routeVO = createRouteSkeleton(origin, task);
        int distance = estimateDistance(origin.point, destination);
        int duration = estimateDurationMinutes(distance);

        routeVO.setAvailable(true);
        routeVO.setSmart(false);
        routeVO.setProvider("fallback");
        routeVO.setMode("DIRECT");
        routeVO.setDistanceMeters(distance);
        routeVO.setDurationMinutes(duration);
        routeVO.setEtaText(buildEtaText(duration));
        routeVO.setNote(StringUtils.hasText(note) ? note : buildFallbackNote(false, null));
        routeVO.setPoints(Arrays.asList(origin.point, destination));

        RouteStepVO step = new RouteStepVO();
        step.setInstruction("Follow the suggested path from pickup point to destination");
        step.setDistanceMeters(distance);
        routeVO.setSteps(Collections.singletonList(step));
        return routeVO;
    }

    private RiderRouteVO buildUnavailableRoute(RiderTaskVO task, RouteOrigin origin) {
        RiderRouteVO routeVO = new RiderRouteVO();
        routeVO.setAvailable(false);
        routeVO.setSmart(false);
        routeVO.setProvider("unavailable");
        routeVO.setSourceType(origin.sourceType);
        routeVO.setSourceLabel(origin.label);
        routeVO.setDestinationLabel(StringUtils.hasText(task.getDeliveryAddress()) ? task.getDeliveryAddress() : "Destination");
        routeVO.setNote("Route planning unavailable because coordinates are missing");
        return routeVO;
    }

    private RiderRouteVO createRouteSkeleton(RouteOrigin origin, RiderTaskVO task) {
        RiderRouteVO routeVO = new RiderRouteVO();
        routeVO.setSourceType(origin.sourceType);
        routeVO.setSourceLabel(origin.label);
        routeVO.setDestinationLabel(StringUtils.hasText(task.getDeliveryAddress()) ? task.getDeliveryAddress() : "Destination");
        return routeVO;
    }

    private List<RoutePointVO> decodePolyline(JSONArray polyline) {
        if (polyline == null || polyline.isEmpty()) {
            return Collections.emptyList();
        }

        List<Double> values = new ArrayList<>();
        for (Object item : polyline) {
            if (item == null) {
                continue;
            }
            try {
                values.add(Double.parseDouble(item.toString()));
            } catch (NumberFormatException ignored) {
                return Collections.emptyList();
            }
        }

        if (values.size() < 2) {
            return Collections.emptyList();
        }

        for (int i = 2; i < values.size(); i++) {
            values.set(i, values.get(i - 2) + values.get(i) / 1000000d);
        }

        List<RoutePointVO> points = new ArrayList<>();
        for (int i = 0; i + 1 < values.size(); i += 2) {
            RoutePointVO point = buildPoint(values.get(i), values.get(i + 1));
            if (point != null) {
                points.add(point);
            }
        }
        return points;
    }

    private List<RoutePointVO> buildRoutePoints(JSONObject route, RoutePointVO origin, RoutePointVO destination) {
        List<RoutePointVO> points = decodePolyline(route.getJSONArray("polyline"));
        if (points.size() > 1) {
            return points;
        }

        List<RoutePointVO> stepPolylinePoints = decodeStepPolylines(route.getJSONArray("steps"));
        if (stepPolylinePoints.size() > 1) {
            return stepPolylinePoints;
        }

        List<RoutePointVO> stepEndpoints = collectStepEndpoints(route.getJSONArray("steps"));
        if (stepEndpoints.size() > 1) {
            return stepEndpoints;
        }

        return Arrays.asList(origin, destination);
    }

    private List<RoutePointVO> decodeStepPolylines(JSONArray steps) {
        if (steps == null || steps.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoutePointVO> merged = new ArrayList<>();
        for (Object item : steps) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject step = (JSONObject) item;
            List<RoutePointVO> stepPoints = decodePolyline(step.getJSONArray("polyline"));
            if (stepPoints.isEmpty()) {
                continue;
            }
            appendWithoutDuplicate(merged, stepPoints);
        }
        return merged;
    }

    private List<RoutePointVO> collectStepEndpoints(JSONArray steps) {
        if (steps == null || steps.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoutePointVO> points = new ArrayList<>();
        for (Object item : steps) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject step = (JSONObject) item;
            appendLocationIfPresent(points, step.getJSONObject("start_location"));
            appendLocationIfPresent(points, step.getJSONObject("end_location"));
        }
        return points;
    }

    private void appendLocationIfPresent(List<RoutePointVO> points, JSONObject location) {
        if (location == null) {
            return;
        }
        RoutePointVO point = buildPoint(location.getDouble("lat"), location.getDouble("lng"));
        if (point == null) {
            return;
        }
        if (points.isEmpty() || !isSamePoint(points.get(points.size() - 1), point)) {
            points.add(point);
        }
    }

    private void appendWithoutDuplicate(List<RoutePointVO> target, List<RoutePointVO> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        if (target.isEmpty()) {
            target.addAll(source);
            return;
        }

        int start = 0;
        if (isSamePoint(target.get(target.size() - 1), source.get(0))) {
            start = 1;
        }
        for (int i = start; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    private boolean isSamePoint(RoutePointVO a, RoutePointVO b) {
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.getLatitude(), b.getLatitude()) && Objects.equals(a.getLongitude(), b.getLongitude());
    }

    private Integer normalizeDurationMinutes(Integer duration, Integer fallbackDistanceMeters) {
        if (duration == null || duration <= 0) {
            return estimateDurationMinutes(fallbackDistanceMeters);
        }
        // Tencent duration is returned in seconds.
        return Math.max(1, (int) Math.ceil(duration / 60d));
    }

    private List<RouteStepVO> parseSteps(JSONArray steps) {
        if (steps == null || steps.isEmpty()) {
            return Collections.emptyList();
        }

        List<RouteStepVO> stepList = new ArrayList<>();
        for (Object item : steps) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject stepObject = (JSONObject) item;
            RouteStepVO stepVO = new RouteStepVO();
            stepVO.setInstruction(stepObject.getStr("instruction"));
            stepVO.setRoadName(stepObject.getStr("road_name"));
            stepVO.setDirection(stepObject.getStr("dir_desc"));
            stepVO.setAction(stepObject.getStr("act_desc"));
            stepVO.setDistanceMeters(stepObject.getInt("distance"));
            stepList.add(stepVO);
        }
        return stepList;
    }

    private String resolveTencentMapKey() {
        if (StringUtils.hasText(tencentMapKey)) {
            return tencentMapKey.trim();
        }

        String envKey = System.getenv("TENCENT_MAP_KEY");
        if (StringUtils.hasText(envKey)) {
            return envKey.trim();
        }

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemConfig::getConfigKey, Arrays.asList(
                "tencent_map_key",
                "qq_map_key",
                "tx_map_key",
                "tencent.map.key",
                "tencentMapKey"
        ));
        return systemConfigMapper.selectList(wrapper).stream()
                .map(SystemConfig::getConfigValue)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse("");
    }

    private String resolveRouteMode() {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, "tencent_map_route_mode")
                .last("limit 1");
        SystemConfig routeModeConfig = systemConfigMapper.selectOne(wrapper);
        if (routeModeConfig != null && StringUtils.hasText(routeModeConfig.getConfigValue())) {
            return routeModeConfig.getConfigValue();
        }
        return defaultRouteMode;
    }

    private int resolveNearbyRadiusMeters() {
        int defaultRadius = 5000;
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, "rider_nearby_radius_m")
                .last("limit 1");
        SystemConfig radiusConfig = systemConfigMapper.selectOne(wrapper);
        if (radiusConfig == null || !StringUtils.hasText(radiusConfig.getConfigValue())) {
            return defaultRadius;
        }
        try {
            int configured = Integer.parseInt(radiusConfig.getConfigValue().trim());
            return configured > 0 ? configured : defaultRadius;
        } catch (Exception ignored) {
            return defaultRadius;
        }
    }

    private String normalizeRouteMode(String routeMode) {
        String normalized = routeMode == null ? "" : routeMode.trim().toLowerCase();
        if ("bicycling".equals(normalized) || "driving".equals(normalized) || "walking".equals(normalized)) {
            return normalized;
        }
        return "ebicycling";
    }

    private String formatPoint(RoutePointVO point) {
        return String.format("%.6f,%.6f", point.getLatitude(), point.getLongitude());
    }

    @SuppressWarnings("unchecked")
    private RoutePointVO resolveRealtimeRiderPoint(Long orderId) {
        try {
            Object raw = redisTemplate.opsForValue().get("rider:location:" + orderId);
            if (!(raw instanceof Map)) {
                return null;
            }
            Map<String, Object> location = (Map<String, Object>) raw;
            Object latitude = location.get("latitude");
            Object longitude = location.get("longitude");
            if (!(latitude instanceof Number) || !(longitude instanceof Number)) {
                return null;
            }
            return buildPoint(((Number) latitude).doubleValue(), ((Number) longitude).doubleValue());
        } catch (Exception ignored) {
            return null;
        }
    }

    private RoutePointVO buildPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        RoutePointVO point = new RoutePointVO();
        point.setLatitude(roundCoordinate(latitude));
        point.setLongitude(roundCoordinate(longitude));
        return point;
    }

    private Double roundCoordinate(Double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    private int estimateDistance(RoutePointVO from, RoutePointVO to) {
        double earthRadius = 6371000d;
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(to.getLongitude() - from.getLongitude());
        double sinLat = Math.sin(deltaLat / 2);
        double sinLng = Math.sin(deltaLng / 2);
        double a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLng * sinLng;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.max(1, (int) Math.round(earthRadius * c));
    }

    private Double roundDistanceKm(int distanceMeters) {
        return BigDecimal.valueOf(distanceMeters / 1000d)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private int estimateDurationMinutes(Integer distanceMeters) {
        int distance = distanceMeters == null ? 0 : distanceMeters;
        if (distance <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(distance / 220d));
    }

    private String buildEtaText(Integer durationMinutes) {
        int duration = durationMinutes == null ? 0 : durationMinutes;
        if (duration <= 0) {
            return "约1分钟";
        }
        return "约" + duration + "分钟";
    }

    private String buildFallbackNote(boolean hasMapKey, String detail) {
        if (!hasMapKey) {
            return "未配置腾讯地图 Key，已切换为直线预估路线";
        }
        if (!StringUtils.hasText(detail)) {
            return "腾讯算路暂时不可用，已切换为直线预估路线";
        }
        return "腾讯算路失败（" + detail + "），已切换为直线预估路线";
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
        return "预计" + minutes + "分钟";
    }

    private Rider ensureRider(Long riderId) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider != null) {
            return rider;
        }

        User user = userMapper.selectById(riderId);
        if (user == null || !"rider".equals(user.getRole())) {
            throw new RuntimeException("Rider not found");
        }

        Rider newRider = new Rider();
        newRider.setId(user.getId());
        newRider.setOpenid(user.getOpenid());
        newRider.setName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "Rider" + user.getId());
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
            throw new RuntimeException("Rider status cannot be empty");
        }
        if (!"online".equals(value) && !"offline".equals(value) && !"delivering".equals(value)) {
            throw new RuntimeException("Invalid rider status");
        }
        return value;
    }

    private static class RouteOrigin {
        private RoutePointVO point;
        private String sourceType;
        private String label;
    }
}
