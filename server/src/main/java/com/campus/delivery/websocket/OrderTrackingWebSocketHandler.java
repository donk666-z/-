package com.campus.delivery.websocket;

import com.campus.delivery.entity.Order;
import com.campus.delivery.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class OrderTrackingWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** orderId -> 订阅该订单连接的会话 */
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> orderSubscribers = new ConcurrentHashMap<>();
    /** session -> 已订阅的 orderId，便于连接关闭时清理 */
    private final Map<WebSocketSession, CopyOnWriteArraySet<Long>> sessionOrders = new ConcurrentHashMap<>();

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionOrders.put(session, new CopyOnWriteArraySet<>());
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

    private void sendEvent(Long orderId, String event, Map<String, Object> payload) {
        CopyOnWriteArraySet<WebSocketSession> subs = orderSubscribers.get(orderId);
        if (subs == null || subs.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("event", event);
            envelope.put("payload", payload);
            String json = objectMapper.writeValueAsString(envelope);
            TextMessage tm = new TextMessage(json);
            for (WebSocketSession session : subs) {
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
