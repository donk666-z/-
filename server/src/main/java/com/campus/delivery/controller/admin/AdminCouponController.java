package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Coupon;
import com.campus.delivery.mapper.CouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/coupon")
public class AdminCouponController {

    @Autowired
    private CouponMapper couponMapper;

    @GetMapping("/list")
    public Result<Page<Coupon>> getList(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String status
    ) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreatedAt);
        Page<Coupon> pageObj = new Page<>(page, size);
        couponMapper.selectPage(pageObj, wrapper);
        return Result.success(pageObj);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Coupon coupon) {
        couponMapper.insert(coupon);
        return Result.success("优惠券创建成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        coupon.setId(id);
        couponMapper.updateById(coupon);
        return Result.success("优惠券更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponMapper.deleteById(id);
        return Result.success("优惠券删除成功", null);
    }
}
