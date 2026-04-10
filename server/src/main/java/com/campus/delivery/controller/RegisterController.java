package com.campus.delivery.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.Rider;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.mapper.RiderMapper;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class RegisterController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private AuthService authService;

    @PostMapping("/merchant-login")
    public Result<Map<String, Object>> merchantLogin(@RequestBody Map<String, String> params) {
        String phone = trim(params.get("phone"));
        String password = trim(params.get("password"));

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            return Result.error("参数不完整");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone).eq(User::getRole, "merchant");
        User user = userMapper.selectOne(wrapper);

        if (user == null || user.getPassword() == null || !BCrypt.checkpw(password, user.getPassword())) {
            return Result.error("手机号或密码错误");
        }
        if (!isUserActive(user)) {
            return Result.error(403, "账号已被禁用，请联系管理员");
        }

        String token = authService.generateToken(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);

        return Result.success("登录成功", data);
    }

    @PostMapping("/merchant-register")
    public Result<Map<String, Object>> merchantRegister(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String password = params.get("password");
        String storeName = params.get("storeName");

        if (phone == null || password == null || storeName == null) {
            return Result.error("参数不完整");
        }

        LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(User::getPhone, phone);
        List<User> existingUsers = userMapper.selectList(checkWrapper);
        if (existingUsers != null && !existingUsers.isEmpty()) {
            User existingUser = existingUsers.stream()
                .filter(u -> phone.equals(u.getPhone()))
                .findFirst().orElse(null);
            if (existingUser != null) {
                return Result.error("该手机号已注册");
            }
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setOpenid("merchant_" + System.currentTimeMillis());
        user.setNickname(storeName);
        user.setRole("merchant");
        user.setStatus("active");
        userMapper.insert(user);

        Merchant merchant = new Merchant();
        merchant.setUserId(user.getId());
        merchant.setName(storeName);
        merchant.setStatus("open");
        merchantMapper.insert(merchant);

        String token = authService.generateToken(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);

        return Result.success("注册成功", data);
    }

    @PostMapping("/rider-login")
    public Result<Map<String, Object>> riderLogin(@RequestBody Map<String, String> params) {
        String phone = trim(params.get("phone"));
        String password = trim(params.get("password"));

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            return Result.error("手机号和密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone).eq(User::getRole, "rider");
        User user = userMapper.selectOne(wrapper);

        if (user == null || user.getPassword() == null || !BCrypt.checkpw(password, user.getPassword())) {
            return Result.error("手机号或密码错误");
        }
        if (!isUserActive(user)) {
            return Result.error(403, "账号已被禁用，请联系管理员");
        }

        ensureRiderProfile(user, null);

        String token = authService.generateToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return Result.success("登录成功", data);
    }

    @PostMapping("/student-login")
    public Result<Map<String, Object>> studentLogin(@RequestBody Map<String, String> params) {
        String phone = trim(params.get("phone"));
        String password = trim(params.get("password"));

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            return Result.error("手机号和密码不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone).eq(User::getRole, "student");
        User user = userMapper.selectOne(wrapper);
        if (user == null || user.getPassword() == null) {
            return Result.error("手机号或密码错误");
        }

        boolean matched;
        try {
            matched = BCrypt.checkpw(password, user.getPassword());
        } catch (Exception ignored) {
            matched = false;
        }
        if (!matched) {
            matched = password.equals(user.getPassword());
        }
        if (!matched) {
            return Result.error("手机号或密码错误");
        }
        if (!isUserActive(user)) {
            return Result.error(403, "账号已被禁用，请联系管理员");
        }

        String token = authService.generateToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return Result.success("登录成功", data);
    }

    @PostMapping("/student-register")
    public Result<Map<String, Object>> studentRegister(@RequestBody Map<String, String> params) {
        String phone = trim(params.get("phone"));
        String password = trim(params.get("password"));
        String nickname = trim(params.get("nickname"));

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            return Result.error("手机号和密码不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }
        if (password.length() < 6) {
            return Result.error("密码至少需要 6 位");
        }

        LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(User::getPhone, phone);
        if (userMapper.selectCount(checkWrapper) > 0) {
            return Result.error("该手机号已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setOpenid("student_" + System.currentTimeMillis());
        user.setNickname(StringUtils.hasText(nickname) ? nickname : "校园用户");
        user.setRole("student");
        user.setStatus("active");
        userMapper.insert(user);

        String token = authService.generateToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return Result.success("注册成功", data);
    }

    @PostMapping("/rider-register")
    public Result<Map<String, Object>> riderRegister(@RequestBody Map<String, String> params) {
        String phone = trim(params.get("phone"));
        String password = trim(params.get("password"));
        String name = trim(params.get("name"));

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password) || !StringUtils.hasText(name)) {
            return Result.error("请完整填写骑手注册信息");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }
        if (password.length() < 6) {
            return Result.error("密码至少需要 6 位");
        }

        LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(User::getPhone, phone);
        if (userMapper.selectCount(checkWrapper) > 0) {
            return Result.error("该手机号已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setOpenid("rider_" + System.currentTimeMillis());
        user.setNickname(name);
        user.setRole("rider");
        user.setStatus("active");
        userMapper.insert(user);

        ensureRiderProfile(user, name);

        String token = authService.generateToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return Result.success("注册成功", data);
    }

    private Rider ensureRiderProfile(User user, String riderName) {
        Rider rider = riderMapper.selectById(user.getId());
        if (rider != null) {
            return rider;
        }

        rider = new Rider();
        rider.setId(user.getId());
        rider.setOpenid(user.getOpenid());
        rider.setName(StringUtils.hasText(riderName) ? riderName : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : "骑手" + user.getId()));
        rider.setPhone(user.getPhone());
        rider.setAvatar(user.getAvatar());
        rider.setStatus("offline");
        rider.setTotalOrders(0);
        rider.setTotalIncome(BigDecimal.ZERO);
        riderMapper.insert(rider);
        return rider;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isUserActive(User user) {
        return user != null && StringUtils.hasText(user.getStatus()) && "active".equalsIgnoreCase(user.getStatus().trim());
    }
}
