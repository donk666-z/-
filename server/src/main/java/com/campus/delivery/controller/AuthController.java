package com.campus.delivery.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.AuthService;
import com.campus.delivery.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody Map<String, String> params) {
        String code = params.get("code");

        User user = authService.wxLogin(code);
        if (user == null || !isUserActive(user)) {
            return Result.error(403, "账号已被禁用，请联系管理员");
        }
        String token = authService.generateToken(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        data.put("role", user.getRole());

        return Result.success("登录成功", data);
    }

    @PostMapping("/admin-login")
    public Result<Map<String, Object>> adminLogin(@RequestBody Map<String, String> params) {
        String username = params == null ? null : params.get("username");
        String password = params == null ? null : params.get("password");
        username = username == null ? null : username.trim();
        password = password == null ? null : password.trim();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.error("用户名或密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, username).eq(User::getRole, "admin");
        User user = userMapper.selectOne(wrapper);

        if (user == null || user.getPassword() == null) {
            return Result.error("用户名或密码错误");
        }
        String storedPassword = user.getPassword().trim();
        boolean matched = false;
        try {
            matched = BCrypt.checkpw(password, storedPassword);
        } catch (Exception ignored) {
            matched = false;
        }
        // 兼容历史明文密码数据，避免初始化脚本与存量数据不一致导致无法登录。
        if (!matched) {
            matched = password.equals(storedPassword);
        }
        if (!matched) {
            return Result.error("用户名或密码错误");
        }
        if (!isUserActive(user)) {
            return Result.error(403, "账号已被禁用，请联系管理员");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getOpenid(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);

        return Result.success("登录成功", data);
    }

    @PostMapping("/admin-register")
    public Result<Map<String, Object>> adminRegister(@RequestBody Map<String, String> params) {
        String username = params == null ? null : params.get("username");
        String password = params == null ? null : params.get("password");
        String nickname = params == null ? null : params.get("nickname");

        username = username == null ? null : username.trim();
        password = password == null ? null : password.trim();
        nickname = nickname == null ? null : nickname.trim();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.error("手机号和密码不能为空");
        }
        if (!username.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }
        if (password.length() < 6) {
            return Result.error("密码至少需要 6 位");
        }

        LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(User::getPhone, username);
        if (userMapper.selectCount(checkWrapper) > 0) {
            return Result.error("该手机号已注册");
        }

        User user = new User();
        user.setPhone(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setOpenid("admin_" + System.currentTimeMillis());
        user.setNickname(StringUtils.hasText(nickname) ? nickname : "管理员");
        user.setRole("admin");
        user.setStatus("active");
        userMapper.insert(user);

        String token = JwtUtil.generateToken(user.getId(), user.getOpenid(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return Result.success("注册成功", data);
    }

    @GetMapping("/user-info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        User user = authService.getUserByToken(token);
        if (user == null || !isUserActive(user)) {
            return Result.error(403, "账号已被禁用，请联系管理员");
        }
        return Result.success(user);
    }

    private boolean isUserActive(User user) {
        return user != null && StringUtils.hasText(user.getStatus()) && "active".equalsIgnoreCase(user.getStatus().trim());
    }
}
