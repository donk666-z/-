package com.campus.delivery.config;

import com.campus.delivery.websocket.JwtWebSocketHandshakeInterceptor;
import com.campus.delivery.websocket.OrderTrackingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private OrderTrackingWebSocketHandler orderTrackingWebSocketHandler;

    @Bean
    public JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor() {
        return new JwtWebSocketHandshakeInterceptor();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderTrackingWebSocketHandler, "/ws")
                .addInterceptors(jwtWebSocketHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
