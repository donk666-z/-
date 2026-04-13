package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Coupon;
import com.campus.delivery.entity.UserCoupon;
import com.campus.delivery.mapper.CouponMapper;
import com.campus.delivery.mapper.UserCouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/coupon")
public class AdminCouponController {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @GetMapping("/list")
    public Result<Page<Coupon>> getList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Coupon::getStatus, status.trim());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Coupon::getName, keyword.trim());
        }
        wrapper.orderByDesc(Coupon::getCreatedAt);
        Page<Coupon> pageObj = new Page<>(page, size);
        couponMapper.selectPage(pageObj, wrapper);
        return Result.success(pageObj);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Coupon coupon) {
        coupon.setId(null);
        coupon.setClaimed(coupon.getClaimed() == null ? 0 : coupon.getClaimed());
        coupon.setStatus(StringUtils.hasText(coupon.getStatus()) ? coupon.getStatus() : "active");
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        couponMapper.insert(coupon);
        return Result.success("优惠券创建成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        Coupon existing = couponMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "优惠券不存在");
        }
        coupon.setId(id);
        coupon.setUpdatedAt(LocalDateTime.now());
        if (coupon.getClaimed() == null) {
            coupon.setClaimed(existing.getClaimed());
        }
        couponMapper.updateById(coupon);
        return Result.success("优惠券更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Coupon existing = couponMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "优惠券不存在");
        }

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getCouponId, id);
        Long referenceCount = userCouponMapper.selectCount(wrapper);
        if (referenceCount != null && referenceCount > 0) {
            return Result.error(400, "该优惠券已发放，不能直接删除，请先停用");
        }

        couponMapper.deleteById(id);
        return Result.success("优惠券删除成功", null);
    }
}
