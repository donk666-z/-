package com.campus.delivery.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.AuthService;
import com.campus.delivery.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Override
    public User wxLogin(String code) {
        // 毕设/演示环境兜底：当 wechat 配置仍是占位值时，直接模拟登录
        // 目的：保证前端下单/支付模拟可以继续演示
        if (isWxMockConfig(appid, secret)) {
            String mockOpenid = "mock_openid";
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getOpenid, mockOpenid);
            User user = userMapper.selectOne(wrapper);
            if (user == null) {
                user = new User();
                user.setOpenid(mockOpenid);
                user.setRole("student");
                user.setStatus("active");
                userMapper.insert(user);
            }
            return user;
        }

        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appid, secret, code
        );

        String response = HttpUtil.get(url);
        JSONObject jsonObject = JSONUtil.parseObj(response);

        String openid = jsonObject.getStr("openid");
        if (openid == null) {
            throw new RuntimeException("微信登录失败");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenid, openid);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setRole("student");
            user.setStatus("active");
            userMapper.insert(user);
        }

        return user;
    }

    /**
     * 判断 wechat 配置是否仍为演示占位值。
     * 当占位值时不调用微信接口，避免“微信登录失败”阻断演示流程。
     */
    private boolean isWxMockConfig(String appid, String secret) {
        if (appid == null || secret == null) return true;
        String a = appid.trim();
        String s = secret.trim();
        if (a.isEmpty() || s.isEmpty()) return true;
        return "your_wx_appid".equals(a) || "your_wx_secret".equals(s);
    }

    @Override
    public String generateToken(User user) {
        return JwtUtil.generateToken(user.getId(), user.getOpenid(), user.getRole());
    }

    @Override
    public User getUserByToken(String token) {
        Long userId = JwtUtil.getUserIdFromToken(token);
        return userMapper.selectById(userId);
    }
}
