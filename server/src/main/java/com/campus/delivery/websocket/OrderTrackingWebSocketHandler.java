package com.campus.delivery.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Order;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class OrderTrackingWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");

    /** orderId -> 订阅该订单连接的会话 */
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> orderSubscribers = new ConcurrentHashMap<>();
    /** session -> 已订阅的 orderId，便于连接关闭时清理 */
    private final Map<WebSocketSession, CopyOnWriteArraySet<Long>> sessionOrders = new ConcurrentHashMap<>();
    /** merchant userId -> 商户连接会话 */
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> merchantSessions = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> riderSessions = new ConcurrentHashMap<>();

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionOrders.put(session, new CopyOnWriteArraySet<>());
        Long userId = (Long) session.getAttributes().get("userId");
        String role = (String) session.getAttributes().get("role");
        if (userId != null && "merchant".equals(role)) {
            merchantSessions.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
        } else if (userId != null && "rider".equals(role)) {
            riderSessions.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        Map<?, ?> msg = objectMapper.readValue(message.getPayload(), Map.class);
        String type = msg.get("type") != null ? msg.get("type").toString() : "";
        if ("heartbeat".equals(type)) {
            return;
        }
        if ("subscribe".equals(type) && msg.get("orderId") != null) {
            long orderId = Long.parseLong(msg.get("orderId").toString());
            Order order = orderMapper.selectById(orderId);
            if (order != null && userId.equals(order.getUserId())) {
                orderSubscribers.computeIfAbsent(orderId, k -> new CopyOnWriteArraySet<>()).add(session);
                sessionOrders.computeIfAbsent(session, k -> new CopyOnWriteArraySet<>()).add(orderId);
            }
            return;
        }
        if ("unsubscribe".equals(type) && msg.get("orderId") != null) {
            long orderId = Long.parseLong(msg.get("orderId").toString());
            unsubscribe(session, orderId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        CopyOnWriteArraySet<Long> orders = sessionOrders.remove(session);
        if (orders != null) {
            for (Long orderId : orders) {
                CopyOnWriteArraySet<WebSocketSession> subs = orderSubscribers.get(orderId);
                if (subs != null) {
                    subs.remove(session);
                    if (subs.isEmpty()) {
                        orderSubscribers.remove(orderId);
                    }
                }
            }
        }
        Long userId = (Long) session.getAttributes().get("userId");
        String role = (String) session.getAttributes().get("role");
        if (userId != null && "merchant".equals(role)) {
            CopyOnWriteArraySet<WebSocketSession> sessions = merchantSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    merchantSessions.remove(userId);
                }
            }
        } else if (userId != null && "rider".equals(role)) {
            CopyOnWriteArraySet<WebSocketSession> sessions = riderSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    riderSessions.remove(userId);
                }
            }
        }
    }

    private void unsubscribe(WebSocketSession session, long orderId) {
        CopyOnWriteArraySet<WebSocketSession> subs = orderSubscribers.get(orderId);
        if (subs != null) {
            subs.remove(session);
            if (subs.isEmpty()) {
                orderSubscribers.remove(orderId);
            }
        }
        CopyOnWriteArraySet<Long> oids = sessionOrders.get(session);
        if (oids != null) {
            oids.remove(orderId);
        }
    }

    public void broadcastRiderLocation(Long orderId, double lat, double lng) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("lat", lat);
        payload.put("lng", lng);
        sendEvent(orderId, "riderLocation", payload);
    }

    public void broadcastOrderStatus(Long orderId, String status, String estimatedTimeText) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("status", status);
        if (estimatedTimeText != null) {
            payload.put("estimatedTime", estimatedTimeText);
        }
        sendEvent(orderId, "orderStatusUpdate", payload);
    }

    public void broadcastMerchantNewOrder(Order order) {
        if (order == null || order.getMerchantId() == null) {
            return;
        }

        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        if (merchant == null || merchant.getUserId() == null) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("merchantId", order.getMerchantId());
        payload.put("totalPrice", order.getTotalPrice());
        payload.put("dishPrice", order.getDishPrice());
        payload.put("deliveryFee", order.getDeliveryFee());
        payload.put("createdAt", order.getCreatedAt() == null ? "" : order.getCreatedAt().format(ORDER_TIME_FORMATTER));

        LambdaQueryWrapper<Order> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Order::getMerchantId, order.getMerchantId())
                .eq(Order::getStatus, "paid");
        payload.put("pendingCount", orderMapper.selectCount(pendingWrapper));

        sendMerchantEvent(merchant.getUserId(), "newOrder", payload);
    }

    public void broadcastRiderOrdersUpdated(String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reason", reason == null ? "updated" : reason);
        sendRiderEventToAll("riderOrdersUpdated", payload);
    }

    public void broadcastRiderAssigned(Long riderId, Long orderId, String message) {
        if (riderId == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("riderId", riderId);
        payload.put("orderId", orderId);
        payload.put("message", message == null ? "系统已为你分配新订单" : message);
        sendRiderEvent(riderId, "riderAssigned", payload);
    }

    private void sendEvent(Long orderId, String event, Map<String, Object> payload) {
        CopyOnWriteArraySet<WebSocketSession> subs = orderSubscribers.get(orderId);
        if (subs == null || subs.isEmpty()) {
            return;
        }
        sendToSessions(subs, event, payload);
    }

    private void sendMerchantEvent(Long merchantUserId, String event, Map<String, Object> payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = merchantSessions.get(merchantUserId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        sendToSessions(sessions, event, payload);
    }

    private void sendRiderEvent(Long riderUserId, String event, Map<String, Object> payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = riderSessions.get(riderUserId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        sendToSessions(sessions, event, payload);
    }

    private void sendRiderEventToAll(String event, Map<String, Object> payload) {
        for (CopyOnWriteArraySet<WebSocketSession> sessions : riderSessions.values()) {
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }
            sendToSessions(sessions, event, payload);
        }
    }

    private void sendToSessions(CopyOnWriteArraySet<WebSocketSession> sessions, String event, Map<String, Object> payload) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("event", event);
            envelope.put("payload", payload);
            String json = objectMapper.writeValueAsString(envelope);
            TextMessage tm = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(tm);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
