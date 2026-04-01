package com.campus.delivery.interceptor;

import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.util.StringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("=== JwtInterceptor ===");
        System.out.println("URI: " + request.getRequestURI());
        System.out.println("Method: " + request.getMethod());
        System.out.println("Authorization: " + request.getHeader("Authorization"));

        if ("OPTIONS".equals(request.getMethod())) {
            System.out.println("OPTIONS skip");
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            System.out.println("Token after substring: " + token.substring(0, Math.min(20, token.length())) + "...");

            if (JwtUtil.validateToken(token)) {
                Long userId = JwtUtil.getUserIdFromToken(token);
                String role = JwtUtil.getRoleFromToken(token);
                User user = userId == null ? null : userMapper.selectById(userId);
                if (user == null) {
                    sendUnauthorized(response, "账号不存在或已被删除");
                    return false;
                }
                if (!StringUtils.hasText(user.getStatus()) || !"active".equalsIgnoreCase(user.getStatus().trim())) {
                    sendForbidden(response, "账号已被禁用，请联系管理员");
                    return false;
                }
                System.out.println("userId: " + userId + ", role: " + role);
                request.setAttribute("userId", userId);
                request.setAttribute("role", role);

                String uri = request.getRequestURI();
                if (uri.startsWith("/api/admin/") && !"admin".equals(role)) {
                    sendForbidden(response, "无权访问管理员接口");
                    return false;
                }
                if (uri.startsWith("/api/merchant/") && !"merchant".equals(role) && !"admin".equals(role)) {
                    sendForbidden(response, "无权访问商户接口");
                    return false;
                }
                if (uri.startsWith("/api/rider/") && !"rider".equals(role) && !"admin".equals(role)) {
                    sendForbidden(response, "无权访问骑手接口");
                    return false;
                }

                System.out.println("Token validated OK");
                return true;
            } else {
                System.out.println("Token validation FAILED");
            }
        } else {
            System.out.println("No Bearer token found");
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未授权，请先登录\"}");
        return false;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }

    private void sendForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\"}");
    }
}
