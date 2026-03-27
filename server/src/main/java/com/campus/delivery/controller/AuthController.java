package com.campus.delivery.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.AuthService;
import com.campus.delivery.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
        String token = authService.generateToken(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        data.put("role", user.getRole());

        return Result.success("登录成功", data);
    }

    @PostMapping("/admin-login")
    public Result<Map<String, Object>> adminLogin(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, username).eq(User::getRole, "admin");
        User user = userMapper.selectOne(wrapper);

        if (user == null || user.getPassword() == null || !BCrypt.checkpw(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getOpenid(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);

        return Result.success("登录成功", data);
    }

    @GetMapping("/user-info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        User user = authService.getUserByToken(token);
        return Result.success(user);
    }
}
