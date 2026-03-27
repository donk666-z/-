package com.campus.delivery.controller.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Coupon;
import com.campus.delivery.entity.UserCoupon;
import com.campus.delivery.mapper.CouponMapper;
import com.campus.delivery.mapper.UserCouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student/coupon")
public class StudentCouponController {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);

        if (userCoupons.isEmpty()) {
            return Result.success(null);
        }

        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toList());

        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);

        List<Map<String, Object>> result = userCoupons.stream().map(uc -> {
            Coupon c = coupons.stream()
                    .filter(coupon -> coupon.getId().equals(uc.getCouponId()))
                    .findFirst()
                    .orElse(null);
            if (c == null) return null;

            Map<String, Object> map = new HashMap<>();
            map.put("id", uc.getId());
            map.put("couponId", c.getId());
            map.put("name", c.getName());
            map.put("type", c.getType());
            map.put("value", c.getDiscount());
            map.put("minAmount", c.getMinAmount());
            map.put("description", c.getType());
            map.put("status", uc.getStatus());
            map.put("expireTime", c.getEndTime());
            return map;
        }).filter(m -> m != null).collect(Collectors.toList());

        return Result.success(result);
    }
}
