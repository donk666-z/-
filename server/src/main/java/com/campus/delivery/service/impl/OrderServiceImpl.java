package com.campus.delivery.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.dto.CreateOrderDTO;
import com.campus.delivery.dto.StudentOrderVO;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.Dish;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Order;
import com.campus.delivery.entity.OrderItem;
import com.campus.delivery.entity.Review;
import com.campus.delivery.entity.Rider;
import com.campus.delivery.entity.SystemConfig;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.OrderItemMapper;
import com.campus.delivery.mapper.OrderMapper;
import com.campus.delivery.mapper.ReviewMapper;
import com.campus.delivery.mapper.RiderMapper;
import com.campus.delivery.mapper.SystemConfigMapper;
import com.campus.delivery.model.ComboConfig;
import com.campus.delivery.model.ComboSnapshot;
import com.campus.delivery.service.DishService;
import com.campus.delivery.service.MerchantService;
import com.campus.delivery.service.OrderRealtimeNotifier;
import com.campus.delivery.service.OrderService;
import com.campus.delivery.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final int DEFAULT_DELIVER_MINUTES = 30;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private DishService dishService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderRealtimeNotifier orderRealtimeNotifier;

    private BigDecimal resolveDeliveryFee(BigDecimal clientFee) {
        BigDecimal fee = new BigDecimal("3.00");
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, "delivery_fee");
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            try {
                fee = new BigDecimal(config.getConfigValue().trim());
            } catch (Exception ignored) {
            }
        }
        if (clientFee != null && clientFee.compareTo(fee) == 0) {
            return fee;
        }
        return fee;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderDTO dto, Long userId) {
        if (dto.getMerchantId() == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(400, "订单参数不完整");
        }

        Merchant merchant = merchantService.getById(dto.getMerchantId());
        if (merchant == null) {
            throw new BusinessException(400, "商户不存在");
        }
        if (!"open".equals(merchant.getStatus())) {
            throw new BusinessException(400, "商户未营业，暂时无法下单");
        }

        BigDecimal dishPrice = BigDecimal.ZERO;
        List<PreparedOrderItem> preparedItems = new ArrayList<>();
        Map<Long, Integer> stockReservations = new HashMap<>();
        Map<Long, Dish> reservedDishMap = new HashMap<>();
        Map<Long, Dish> dishCache = new HashMap<>();

        for (CreateOrderDTO.OrderItemDTO itemDTO : dto.getItems()) {
            PreparedOrderItem preparedItem = prepareOrderItem(
                    itemDTO,
                    dto.getMerchantId(),
                    stockReservations,
                    reservedDishMap,
                    dishCache
            );
            preparedItems.add(preparedItem);
            dishPrice = dishPrice.add(
                    preparedItem.getUnitPrice().multiply(BigDecimal.valueOf(preparedItem.getQuantity()))
            );
        }

        BigDecimal deliveryFee = resolveDeliveryFee(dto.getDeliveryFee());
        BigDecimal totalPrice = dishPrice.add(deliveryFee);

        Order order = new Order();
        order.setOrderNo("ORD" + IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setMerchantId(dto.getMerchantId());
        order.setDishPrice(dishPrice);
        order.setDeliveryFee(deliveryFee);
        order.setTotalPrice(totalPrice);
        order.setAddress(dto.getAddress());
        order.setPhone(dto.getPhone());
        order.setRemark(dto.getRemark());
        order.setStatus("paid");
        order.setPaymentMethod("mock");
        order.setPaymentTime(LocalDateTime.now());
        save(order);

        for (PreparedOrderItem preparedItem : preparedItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setDishId(preparedItem.getDish().getId());
            orderItem.setDishName(preparedItem.getDish().getName());
            orderItem.setDishImage(preparedItem.getDish().getImage());
            orderItem.setPrice(preparedItem.getUnitPrice());
            orderItem.setQuantity(preparedItem.getQuantity());
            orderItem.setItemType(preparedItem.getItemType());
            orderItem.setComboSnapshot(preparedItem.getComboSnapshot());
            orderItem.setComboSnapshotJson(writeComboSnapshot(preparedItem.getComboSnapshot()));
            orderItemMapper.insert(orderItem);
        }

        for (Map.Entry<Long, Integer> entry : stockReservations.entrySet()) {
            Dish dish = reservedDishMap.get(entry.getKey());
            if (dish == null) {
                continue;
            }
            int currentStock = dish.getStock() == null ? 0 : dish.getStock();
            dish.setStock(currentStock - entry.getValue());
            dishService.updateById(dish);
        }

        return order;
    }

    @Override
    public List<StudentOrderVO> getStudentOrderList(Long userId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);

        if (status != null && !status.isEmpty()) {
            if ("pending".equals(status)) {
                wrapper.eq(Order::getStatus, "pending");
            } else if ("paid".equals(status)) {
                wrapper.eq(Order::getStatus, "paid");
            } else if ("processing".equals(status)) {
                wrapper.in(Order::getStatus, Arrays.asList("paid", "accepted", "prepared", "delivering"));
            } else if ("delivering".equals(status)) {
                wrapper.eq(Order::getStatus, "delivering");
            } else if ("completed".equals(status)) {
                wrapper.eq(Order::getStatus, "completed");
            } else if ("cancelled".equals(status)) {
                wrapper.eq(Order::getStatus, "cancelled");
            }
        }

        wrapper.orderByDesc(Order::getCreatedAt);
        List<Order> orders = list(wrapper);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> merchantIds = orders.stream().map(Order::getMerchantId).collect(Collectors.toSet());
        Map<Long, String> merchantNames = merchantService.listByIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName));

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        Map<Long, List<OrderItem>> itemsByOrder = loadOrderItemsByOrderIds(orderIds);

        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.in(Review::getOrderId, orderIds);
        Set<Long> reviewedIds = reviewMapper.selectList(reviewWrapper).stream()
                .map(Review::getOrderId)
                .collect(Collectors.toSet());

        List<StudentOrderVO> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(toListVo(
                    order,
                    merchantNames.get(order.getMerchantId()),
                    itemsByOrder.getOrDefault(order.getId(), Collections.emptyList()),
                    reviewedIds.contains(order.getId())
            ));
        }
        return result;
    }

    @Override
    public StudentOrderVO getStudentOrderDetail(Long id, Long userId) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该订单");
        }

        Merchant merchant = merchantService.getById(order.getMerchantId());

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        enrichOrderItems(items);

        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getOrderId, order.getId());
        boolean reviewed = reviewMapper.selectCount(reviewWrapper) > 0;

        Rider rider = order.getRiderId() != null ? riderMapper.selectById(order.getRiderId()) : null;
        Address address = resolveAddressFromOrder(userId, order.getAddress());

        return toDetailVo(order, merchant != null ? merchant.getName() : "", items, reviewed, rider, address);
    }

    @Override
    public List<StudentOrderVO> getMerchantOrderList(Long merchantId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getMerchantId, merchantId);

        String normalizedStatus = normalizeMerchantStatus(status);
        if (normalizedStatus != null && !normalizedStatus.isEmpty()) {
            wrapper.eq(Order::getStatus, normalizedStatus);
        }

        wrapper.orderByDesc(Order::getCreatedAt);
        List<Order> orders = list(wrapper);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<OrderItem>> itemsByOrder = loadOrderItemsByOrderIds(
                orders.stream().map(Order::getId).collect(Collectors.toList())
        );

        List<StudentOrderVO> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(toMerchantVo(order, itemsByOrder.getOrDefault(order.getId(), Collections.emptyList())));
        }
        return result;
    }

    @Override
    public StudentOrderVO getMerchantOrderDetail(Long id, Long merchantId) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!merchantId.equals(order.getMerchantId())) {
            throw new BusinessException(403, "无权查看该订单");
        }

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        enrichOrderItems(items);

        Rider rider = order.getRiderId() != null ? riderMapper.selectById(order.getRiderId()) : null;
        return toDetailVo(order, "", items, false, rider, null);
    }

    @Override
    public Order getOrderDetail(Long id) {
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        Order existing = getById(id);
        if (existing == null) {
            throw new BusinessException("订单不存在");
        }
        if ("cancelled".equals(existing.getStatus())) {
            throw new BusinessException(400, "订单已取消");
        }
        restoreStock(id);
        Order order = new Order();
        order.setId(id);
        order.setStatus("cancelled");
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void studentCancelOrder(Long id, Long userId) {
        Order existing = getById(id);
        if (existing == null) {
            throw new BusinessException("订单不存在");
        }
        if ("cancelled".equals(existing.getStatus())) {
            throw new BusinessException(400, "订单已取消");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        if (!Arrays.asList("pending", "paid").contains(existing.getStatus())) {
            throw new BusinessException(400, "当前状态不可取消");
        }
        restoreStock(id);
        Order order = new Order();
        order.setId(id);
        order.setStatus("cancelled");
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void studentConfirmReceipt(Long id, Long userId) {
        Order existing = getById(id);
        if (existing == null) {
            throw new BusinessException("订单不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        if (!"delivered".equals(existing.getStatus())) {
            throw new BusinessException(400, "请等待送达后再确认收货");
        }
        Order order = new Order();
        order.setId(id);
        order.setStatus("completed");
        order.setCompletedTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public void acceptOrder(Long id) {
        Order existing = getById(id);
        if (existing == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"paid".equals(existing.getStatus())) {
            throw new BusinessException(400, "当前订单不是待接单状态");
        }
        Order order = new Order();
        order.setId(id);
        order.setStatus("accepted");
        updateById(order);
        orderRealtimeNotifier.notifyOrderStatus(id, "accepted", null);
    }

    @Override
    public void prepareOrder(Long id) {
        Order existing = getById(id);
        if (existing == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"accepted".equals(existing.getStatus())) {
            throw new BusinessException(400, "当前订单不是已接单状态");
        }
        Order order = new Order();
        order.setId(id);
        order.setStatus("prepared");
        updateById(order);
        orderRealtimeNotifier.notifyOrderStatus(id, "prepared", null);
    }

    @Override
    public void pickupOrder(Long id, Long riderId) {
        Order existing = getById(id);
        if (existing == null) {
            throw new BusinessException("订单不存在");
        }
        if (existing.getRiderId() == null || !existing.getRiderId().equals(riderId)) {
            throw new BusinessException(403, "无权配送该订单");
        }
        if (!"prepared".equals(existing.getStatus())) {
            throw new BusinessException(400, "当前状态不可开始配送");
        }
        Order order = new Order();
        order.setId(id);
        order.setStatus("delivering");
        order.setRiderId(riderId);
        order.setEstimatedTime(DEFAULT_DELIVER_MINUTES);
        updateById(order);
        String eta = "约 " + DEFAULT_DELIVER_MINUTES + " 分钟";
        orderRealtimeNotifier.notifyOrderStatus(id, "delivering", eta);
    }

    @Override
    public void deliverOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setStatus("delivered");
        updateById(order);
        orderRealtimeNotifier.notifyOrderStatus(id, "delivered", null);
    }

    @Override
    public List<Order> getOrdersByMerchant(Long merchantId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getMerchantId, merchantId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public List<Order> getOrdersByRider(Long riderId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getRiderId, riderId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> getMerchantStats(Long merchantId) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        LambdaQueryWrapper<Order> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(Order::getMerchantId, merchantId)
                .ge(Order::getCreatedAt, todayStart)
                .eq(Order::getStatus, "completed");
        stats.put("todayOrders", count(todayWrapper));

        LambdaQueryWrapper<Order> revenueWrapper = new LambdaQueryWrapper<>();
        revenueWrapper.eq(Order::getMerchantId, merchantId)
                .ge(Order::getCreatedAt, todayStart)
                .eq(Order::getStatus, "completed");
        List<Order> todayCompleted = list(revenueWrapper);
        BigDecimal todayRevenue = todayCompleted.stream()
                .map(Order::getDishPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayRevenue", todayRevenue);

        LambdaQueryWrapper<Order> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Order::getMerchantId, merchantId)
                .eq(Order::getStatus, "paid");
        stats.put("pendingOrders", count(pendingWrapper));

        return stats;
    }

    @Override
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        stats.put("totalOrders", count());

        LambdaQueryWrapper<Order> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(Order::getCreatedAt, todayStart);
        stats.put("todayOrders", count(todayWrapper));

        LambdaQueryWrapper<Order> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Order::getStatus, "completed")
                .ge(Order::getCreatedAt, todayStart);
        List<Order> todayCompleted = list(completedWrapper);
        BigDecimal todayRevenue = todayCompleted.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayRevenue", todayRevenue);

        return stats;
    }

    private PreparedOrderItem prepareOrderItem(CreateOrderDTO.OrderItemDTO itemDTO,
                                               Long merchantId,
                                               Map<Long, Integer> stockReservations,
                                               Map<Long, Dish> reservedDishMap,
                                               Map<Long, Dish> dishCache) {
        if (itemDTO.getDishId() == null || itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
            throw new BusinessException(400, "购物车数据无效");
        }

        Dish dish = loadDish(itemDTO.getDishId(), dishCache);
        if (dish == null || !merchantId.equals(dish.getMerchantId())) {
            throw new BusinessException(400, "菜品不存在或不属于该商户");
        }
        if (!"available".equals(dish.getStatus())) {
            throw new BusinessException(400, "菜品“" + dish.getName() + "”已下架");
        }

        if (DishService.TYPE_COMBO.equals(normalizeDishType(dish.getType()))) {
            return prepareComboOrderItem(dish, itemDTO, stockReservations, reservedDishMap, dishCache);
        }
        return prepareSingleOrderItem(dish, itemDTO, stockReservations, reservedDishMap);
    }

    private PreparedOrderItem prepareSingleOrderItem(Dish dish,
                                                     CreateOrderDTO.OrderItemDTO itemDTO,
                                                     Map<Long, Integer> stockReservations,
                                                     Map<Long, Dish> reservedDishMap) {
        reserveDishStock(dish, itemDTO.getQuantity(), stockReservations, reservedDishMap);

        PreparedOrderItem preparedItem = new PreparedOrderItem();
        preparedItem.setDish(dish);
        preparedItem.setItemType(DishService.TYPE_SINGLE);
        preparedItem.setQuantity(itemDTO.getQuantity());
        preparedItem.setUnitPrice(dish.getPrice() == null ? BigDecimal.ZERO : dish.getPrice());
        return preparedItem;
    }

    private PreparedOrderItem prepareComboOrderItem(Dish comboDish,
                                                    CreateOrderDTO.OrderItemDTO itemDTO,
                                                    Map<Long, Integer> stockReservations,
                                                    Map<Long, Dish> reservedDishMap,
                                                    Map<Long, Dish> dishCache) {
        ComboConfig comboConfig = dishService.parseComboConfig(comboDish);
        if (comboConfig == null || comboConfig.getGroups() == null || comboConfig.getGroups().isEmpty()) {
            throw new BusinessException(400, "套餐配置不存在或已失效");
        }

        BigDecimal unitPrice = comboDish.getPrice() == null ? BigDecimal.ZERO : comboDish.getPrice();
        ComboSnapshot comboSnapshot = new ComboSnapshot();
        List<ComboSnapshot.Group> snapshotGroups = new ArrayList<>();

        for (int groupIndex = 0; groupIndex < comboConfig.getGroups().size(); groupIndex++) {
            ComboConfig.Group group = comboConfig.getGroups().get(groupIndex);
            Map<Long, ComboConfig.Option> optionMap = buildOptionMap(group);

            CreateOrderDTO.ComboSelectionGroupDTO selectedGroup = resolveSelectionGroup(
                    itemDTO.getComboSelections(),
                    groupIndex,
                    group.getName()
            );
            List<CreateOrderDTO.ComboSelectionOptionDTO> selectedOptions = selectedGroup == null || selectedGroup.getOptions() == null
                    ? Collections.emptyList()
                    : selectedGroup.getOptions();

            validateSelectionCount(group, selectedOptions.size());

            Set<Long> selectedDishIds = new HashSet<>();
            List<ComboSnapshot.Option> snapshotOptions = new ArrayList<>();
            for (CreateOrderDTO.ComboSelectionOptionDTO selectedOption : selectedOptions) {
                if (selectedOption == null || selectedOption.getDishId() == null) {
                    throw new BusinessException(400, "套餐选项无效");
                }
                if (!selectedDishIds.add(selectedOption.getDishId())) {
                    throw new BusinessException(400, "套餐分组“" + group.getName() + "”存在重复选项");
                }

                ComboConfig.Option configuredOption = optionMap.get(selectedOption.getDishId());
                if (configuredOption == null) {
                    throw new BusinessException(400, "套餐选项已变更，请重新选择");
                }

                Dish optionDish = loadDish(configuredOption.getDishId(), dishCache);
                if (optionDish == null || !comboDish.getMerchantId().equals(optionDish.getMerchantId())) {
                    throw new BusinessException(400, "套餐选项菜品不存在");
                }
                if (!DishService.TYPE_SINGLE.equals(normalizeDishType(optionDish.getType()))) {
                    throw new BusinessException(400, "套餐选项只能是单品");
                }
                if (!"available".equals(optionDish.getStatus())) {
                    throw new BusinessException(400, "套餐选项“" + optionDish.getName() + "”暂不可选");
                }

                int optionQuantity = configuredOption.getQuantity() == null || configuredOption.getQuantity() <= 0
                        ? 1
                        : configuredOption.getQuantity();
                reserveDishStock(optionDish, optionQuantity * itemDTO.getQuantity(), stockReservations, reservedDishMap);

                BigDecimal extraPrice = configuredOption.getExtraPrice() == null
                        ? BigDecimal.ZERO
                        : configuredOption.getExtraPrice();
                unitPrice = unitPrice.add(extraPrice);

                ComboSnapshot.Option snapshotOption = new ComboSnapshot.Option();
                snapshotOption.setDishId(optionDish.getId());
                snapshotOption.setDishName(optionDish.getName());
                snapshotOption.setDishImage(optionDish.getImage());
                snapshotOption.setQuantity(optionQuantity);
                snapshotOption.setExtraPrice(extraPrice);
                snapshotOption.setDishPrice(optionDish.getPrice());
                snapshotOptions.add(snapshotOption);
            }

            if (!snapshotOptions.isEmpty()) {
                ComboSnapshot.Group snapshotGroup = new ComboSnapshot.Group();
                snapshotGroup.setGroupIndex(groupIndex);
                snapshotGroup.setName(group.getName());
                snapshotGroup.setMinSelect(group.getMinSelect());
                snapshotGroup.setMaxSelect(group.getMaxSelect());
                snapshotGroup.setOptions(snapshotOptions);
                snapshotGroups.add(snapshotGroup);
            }
        }

        comboSnapshot.setGroups(snapshotGroups);

        PreparedOrderItem preparedItem = new PreparedOrderItem();
        preparedItem.setDish(comboDish);
        preparedItem.setItemType(DishService.TYPE_COMBO);
        preparedItem.setQuantity(itemDTO.getQuantity());
        preparedItem.setUnitPrice(unitPrice);
        preparedItem.setComboSnapshot(comboSnapshot);
        return preparedItem;
    }

    private Map<Long, ComboConfig.Option> buildOptionMap(ComboConfig.Group group) {
        if (group == null || group.getOptions() == null) {
            return Collections.emptyMap();
        }
        return group.getOptions().stream()
                .filter(Objects::nonNull)
                .filter(option -> option.getDishId() != null)
                .collect(Collectors.toMap(
                        ComboConfig.Option::getDishId,
                        option -> option,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private CreateOrderDTO.ComboSelectionGroupDTO resolveSelectionGroup(List<CreateOrderDTO.ComboSelectionGroupDTO> selections,
                                                                        int groupIndex,
                                                                        String groupName) {
        if (selections == null || selections.isEmpty()) {
            return null;
        }

        for (CreateOrderDTO.ComboSelectionGroupDTO selection : selections) {
            if (selection != null && selection.getGroupIndex() != null && selection.getGroupIndex() == groupIndex) {
                return selection;
            }
        }
        for (CreateOrderDTO.ComboSelectionGroupDTO selection : selections) {
            if (selection != null
                    && StringUtils.hasText(selection.getName())
                    && selection.getName().trim().equals(groupName)) {
                return selection;
            }
        }
        if (groupIndex >= 0 && groupIndex < selections.size()) {
            CreateOrderDTO.ComboSelectionGroupDTO fallback = selections.get(groupIndex);
            if (fallback != null && fallback.getGroupIndex() == null) {
                return fallback;
            }
        }
        return null;
    }

    private void validateSelectionCount(ComboConfig.Group group, int selectedCount) {
        int minSelect = group.getMinSelect() == null ? 0 : group.getMinSelect();
        int maxSelect = group.getMaxSelect() == null ? 0 : group.getMaxSelect();
        if (selectedCount < minSelect) {
            throw new BusinessException(400, "套餐分组“" + group.getName() + "”至少选择 " + minSelect + " 项");
        }
        if (maxSelect > 0 && selectedCount > maxSelect) {
            throw new BusinessException(400, "套餐分组“" + group.getName() + "”最多选择 " + maxSelect + " 项");
        }
    }

    private void reserveDishStock(Dish dish,
                                  int quantity,
                                  Map<Long, Integer> stockReservations,
                                  Map<Long, Dish> reservedDishMap) {
        if (quantity <= 0) {
            return;
        }
        int currentReserved = stockReservations.getOrDefault(dish.getId(), 0);
        int nextReserved = currentReserved + quantity;
        int stock = dish.getStock() == null ? 0 : dish.getStock();
        if (stock < nextReserved) {
            throw new BusinessException(400, "“" + dish.getName() + "”库存不足");
        }
        stockReservations.put(dish.getId(), nextReserved);
        reservedDishMap.put(dish.getId(), dish);
    }

    private Dish loadDish(Long dishId, Map<Long, Dish> dishCache) {
        if (dishId == null) {
            return null;
        }
        Dish cached = dishCache.get(dishId);
        if (cached != null) {
            return cached;
        }
        Dish dish = dishService.getById(dishId);
        if (dish != null) {
            dishCache.put(dishId, dish);
        }
        return dish;
    }

    private Address resolveAddressFromOrder(Long userId, String orderAddressLine) {
        if (orderAddressLine == null) {
            return null;
        }
        String norm = orderAddressLine.replaceAll("\\s+", " ").trim();
        for (Address address : userService.getAddresses(userId)) {
            String line = (nullToEmpty(address.getAddress()) + " "
                    + nullToEmpty(address.getBuilding()) + " "
                    + nullToEmpty(address.getRoom()))
                    .replaceAll("\\s+", " ")
                    .trim();
            if (norm.equals(line)) {
                return address;
            }
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Map<Long, List<OrderItem>> loadOrderItemsByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderItem::getOrderId, orderIds);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        enrichOrderItems(items);
        return items.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private void enrichOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OrderItem item : items) {
            item.setItemType(normalizeItemType(item.getItemType()));
            item.setComboSnapshot(parseComboSnapshot(item.getComboSnapshotJson()));
        }
    }

    private ComboSnapshot parseComboSnapshot(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(rawJson, ComboSnapshot.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String writeComboSnapshot(ComboSnapshot comboSnapshot) {
        if (comboSnapshot == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(comboSnapshot);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "套餐快照保存失败");
        }
    }

    private StudentOrderVO toListVo(Order order, String merchantName, List<OrderItem> items, boolean reviewed) {
        StudentOrderVO vo = new StudentOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setMerchantId(order.getMerchantId());
        vo.setMerchantName(merchantName);
        vo.setStatus(order.getStatus());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setReviewed(reviewed);
        vo.setDishes(toDishVos(items));
        return vo;
    }

    private StudentOrderVO toMerchantVo(Order order, List<OrderItem> items) {
        StudentOrderVO vo = new StudentOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setMerchantId(order.getMerchantId());
        vo.setRiderId(order.getRiderId());
        vo.setDishPrice(order.getDishPrice());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setAddress(order.getAddress());
        vo.setPhone(order.getPhone());
        vo.setRemark(order.getRemark());
        vo.setStatus(order.getStatus());
        vo.setEstimatedTime(formatEstimated(order.getEstimatedTime()));
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setPaymentTime(order.getPaymentTime());
        vo.setCompletedTime(order.getCompletedTime());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setDishes(toDishVos(items));
        return vo;
    }

    private StudentOrderVO toDetailVo(Order order,
                                      String merchantName,
                                      List<OrderItem> items,
                                      boolean reviewed,
                                      Rider rider,
                                      Address address) {
        StudentOrderVO vo = new StudentOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setMerchantId(order.getMerchantId());
        vo.setMerchantName(merchantName);
        vo.setRiderId(order.getRiderId());
        vo.setDishPrice(order.getDishPrice());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setAddress(order.getAddress());
        vo.setPhone(order.getPhone());
        vo.setRemark(order.getRemark());
        vo.setStatus(order.getStatus());
        vo.setEstimatedTime(formatEstimated(order.getEstimatedTime()));
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setPaymentTime(order.getPaymentTime());
        vo.setCompletedTime(order.getCompletedTime());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setReviewed(reviewed);
        vo.setDishes(toDishVos(items));

        if (rider != null) {
            vo.setRiderName(rider.getName());
            vo.setRiderPhone(rider.getPhone());
            if (rider.getLatitude() != null) {
                vo.setRiderLat(rider.getLatitude().doubleValue());
            }
            if (rider.getLongitude() != null) {
                vo.setRiderLng(rider.getLongitude().doubleValue());
            }
        }

        if (address != null) {
            if (address.getLatitude() != null) {
                vo.setAddressLat(address.getLatitude().doubleValue());
            }
            if (address.getLongitude() != null) {
                vo.setAddressLng(address.getLongitude().doubleValue());
            }
        }

        applyRiderLocationFromRedis(order, vo);
        return vo;
    }

    @SuppressWarnings("unchecked")
    private void applyRiderLocationFromRedis(Order order, StudentOrderVO vo) {
        if (!"delivering".equals(order.getStatus())) {
            return;
        }
        try {
            Object raw = redisTemplate.opsForValue().get("rider:location:" + order.getId());
            if (raw instanceof Map) {
                Map<String, Object> location = (Map<String, Object>) raw;
                Object latitude = location.get("latitude");
                Object longitude = location.get("longitude");
                if (latitude instanceof Number && longitude instanceof Number) {
                    vo.setRiderLat(((Number) latitude).doubleValue());
                    vo.setRiderLng(((Number) longitude).doubleValue());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String formatEstimated(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return null;
        }
        return "约 " + minutes + " 分钟";
    }

    private String normalizeMerchantStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "pending":
            case "paid":
                return "paid";
            case "accepted":
                return "accepted";
            case "prepared":
                return "prepared";
            case "delivering":
                return "delivering";
            case "delivered":
                return "delivered";
            case "completed":
                return "completed";
            case "cancelled":
            case "canceled":
                return "cancelled";
            default:
                return normalized;
        }
    }

    private List<StudentOrderVO.OrderDishVO> toDishVos(List<OrderItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<StudentOrderVO.OrderDishVO> list = new ArrayList<>();
        for (OrderItem item : items) {
            StudentOrderVO.OrderDishVO dishVO = new StudentOrderVO.OrderDishVO();
            dishVO.setId(item.getDishId());
            dishVO.setName(item.getDishName());
            dishVO.setImage(item.getDishImage());
            dishVO.setType(normalizeItemType(item.getItemType()));
            dishVO.setPrice(item.getPrice());
            dishVO.setQuantity(item.getQuantity());
            dishVO.setComboGroups(toComboGroups(item.getComboSnapshot(), item.getQuantity()));
            list.add(dishVO);
        }
        return list;
    }

    private List<StudentOrderVO.ComboGroupVO> toComboGroups(ComboSnapshot comboSnapshot, Integer parentQuantity) {
        if (comboSnapshot == null || comboSnapshot.getGroups() == null || comboSnapshot.getGroups().isEmpty()) {
            return Collections.emptyList();
        }
        int comboCount = parentQuantity == null || parentQuantity <= 0 ? 1 : parentQuantity;
        List<StudentOrderVO.ComboGroupVO> groups = new ArrayList<>();
        for (ComboSnapshot.Group group : comboSnapshot.getGroups()) {
            StudentOrderVO.ComboGroupVO groupVO = new StudentOrderVO.ComboGroupVO();
            groupVO.setGroupIndex(group.getGroupIndex());
            groupVO.setName(group.getName());

            List<StudentOrderVO.ComboOptionVO> optionVos = new ArrayList<>();
            if (group.getOptions() != null) {
                for (ComboSnapshot.Option option : group.getOptions()) {
                    StudentOrderVO.ComboOptionVO optionVO = new StudentOrderVO.ComboOptionVO();
                    optionVO.setDishId(option.getDishId());
                    optionVO.setDishName(option.getDishName());
                    optionVO.setDishImage(option.getDishImage());
                    optionVO.setQuantity(option.getQuantity());
                    optionVO.setTotalQuantity((option.getQuantity() == null ? 0 : option.getQuantity()) * comboCount);
                    optionVO.setExtraPrice(option.getExtraPrice());
                    optionVos.add(optionVO);
                }
            }
            groupVO.setOptions(optionVos);
            groups.add(groupVO);
        }
        return groups;
    }

    private void restoreStock(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        enrichOrderItems(items);
        for (OrderItem item : items) {
            if (DishService.TYPE_COMBO.equals(normalizeItemType(item.getItemType()))) {
                restoreComboItemStock(item);
                continue;
            }

            Dish dish = dishService.getById(item.getDishId());
            if (dish != null) {
                int stock = dish.getStock() == null ? 0 : dish.getStock();
                dish.setStock(stock + item.getQuantity());
                dishService.updateById(dish);
            }
        }
    }

    private void restoreComboItemStock(OrderItem item) {
        ComboSnapshot comboSnapshot = item.getComboSnapshot();
        if (comboSnapshot == null || comboSnapshot.getGroups() == null) {
            return;
        }
        int comboCount = item.getQuantity() == null ? 0 : item.getQuantity();
        for (ComboSnapshot.Group group : comboSnapshot.getGroups()) {
            if (group.getOptions() == null) {
                continue;
            }
            for (ComboSnapshot.Option option : group.getOptions()) {
                if (option == null || option.getDishId() == null || option.getQuantity() == null || option.getQuantity() <= 0) {
                    continue;
                }
                Dish dish = dishService.getById(option.getDishId());
                if (dish == null) {
                    continue;
                }
                int stock = dish.getStock() == null ? 0 : dish.getStock();
                dish.setStock(stock + option.getQuantity() * comboCount);
                dishService.updateById(dish);
            }
        }
    }

    private String normalizeDishType(String dishType) {
        if (!StringUtils.hasText(dishType)) {
            return DishService.TYPE_SINGLE;
        }
        return DishService.TYPE_COMBO.equalsIgnoreCase(dishType)
                ? DishService.TYPE_COMBO
                : DishService.TYPE_SINGLE;
    }

    private String normalizeItemType(String itemType) {
        if (!StringUtils.hasText(itemType)) {
            return DishService.TYPE_SINGLE;
        }
        return DishService.TYPE_COMBO.equalsIgnoreCase(itemType)
                ? DishService.TYPE_COMBO
                : DishService.TYPE_SINGLE;
    }

    private static class PreparedOrderItem {
        private Dish dish;
        private String itemType;
        private BigDecimal unitPrice;
        private Integer quantity;
        private ComboSnapshot comboSnapshot;

        public Dish getDish() {
            return dish;
        }

        public void setDish(Dish dish) {
            this.dish = dish;
        }

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public ComboSnapshot getComboSnapshot() {
            return comboSnapshot;
        }

        public void setComboSnapshot(ComboSnapshot comboSnapshot) {
            this.comboSnapshot = comboSnapshot;
        }
    }
}
