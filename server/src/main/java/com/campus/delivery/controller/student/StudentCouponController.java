package com.campus.delivery.controller.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.dto.PromotionCenterItemVO;
import com.campus.delivery.entity.Coupon;
import com.campus.delivery.entity.PromotionActivity;
import com.campus.delivery.entity.UserCoupon;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.CouponMapper;
import com.campus.delivery.mapper.PromotionActivityMapper;
import com.campus.delivery.mapper.UserCouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student/coupon")
public class StudentCouponController {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private PromotionActivityMapper promotionActivityMapper;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>();
        wrapper.eq(UserCoupon::getUserId, userId).orderByDesc(UserCoupon::getCreatedAt);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
        if (userCoupons.isEmpty()) {
            return Result.success(Collections.<Map<String, Object>>emptyList());
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
            if (c == null) {
                return null;
            }

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", uc.getId());
            map.put("couponId", c.getId());
            map.put("promotionId", uc.getPromotionId());
            map.put("name", c.getName());
            map.put("type", c.getType());
            map.put("value", c.getDiscount());
            map.put("minAmount", c.getMinAmount());
            map.put("description", c.getType());
            map.put("status", uc.getStatus());
            map.put("expireTime", c.getEndTime());
            return map;
        }).filter(item -> item != null).collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/center")
    public Result<List<PromotionCenterItemVO>> getCenter(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<PromotionActivity> wrapper = new LambdaQueryWrapper<PromotionActivity>();
        wrapper.eq(PromotionActivity::getStatus, "active")
                .le(PromotionActivity::getStartTime, now)
                .ge(PromotionActivity::getEndTime, now)
                .orderByDesc(PromotionActivity::getCreatedAt);
        List<PromotionActivity> activities = promotionActivityMapper.selectList(wrapper);
        if (activities.isEmpty()) {
            return Result.success(Collections.<PromotionCenterItemVO>emptyList());
        }

        Set<Long> couponIds = activities.stream()
                .map(PromotionActivity::getCouponId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, item -> item));

        Set<Long> promotionIds = activities.stream()
                .map(PromotionActivity::getId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<UserCoupon> claimWrapper = new LambdaQueryWrapper<UserCoupon>();
        claimWrapper.eq(UserCoupon::getUserId, userId).in(UserCoupon::getPromotionId, promotionIds);
        Map<Long, Long> claimCountMap = userCouponMapper.selectList(claimWrapper).stream()
                .filter(item -> item.getPromotionId() != null)
                .collect(Collectors.groupingBy(UserCoupon::getPromotionId, Collectors.counting()));

        List<PromotionCenterItemVO> result = new ArrayList<PromotionCenterItemVO>();
        for (PromotionActivity activity : activities) {
            Coupon coupon = couponMap.get(activity.getCouponId());
            if (coupon == null || !"active".equals(coupon.getStatus())) {
                continue;
            }

            PromotionCenterItemVO item = new PromotionCenterItemVO();
            item.setPromotionId(activity.getId());
            item.setPromotionName(activity.getName());
            item.setCouponId(coupon.getId());
            item.setCouponName(coupon.getName());
            item.setType(coupon.getType());
            item.setDiscount(coupon.getDiscount());
            item.setMinAmount(coupon.getMinAmount());
            item.setEndTime(activity.getEndTime());
            item.setDescription(activity.getDescription());
            item.setClaimStatus(resolveClaimStatus(activity, coupon, claimCountMap.get(activity.getId())));
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping("/claim/{promotionId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> claim(@PathVariable Long promotionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDateTime now = LocalDateTime.now();

        PromotionActivity activity = promotionActivityMapper.selectById(promotionId);
        if (activity == null) {
            throw new BusinessException(404, "促销活动不存在");
        }
        if (!"active".equals(activity.getStatus())) {
            throw new BusinessException(400, "当前活动不可领取");
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null
                || now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException(400, "当前活动不在领取时间内");
        }

        Coupon coupon = couponMapper.selectById(activity.getCouponId());
        if (coupon == null) {
            throw new BusinessException(404, "关联优惠券不存在");
        }
        if (!"active".equals(coupon.getStatus())) {
            throw new BusinessException(400, "当前优惠券不可领取");
        }
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new BusinessException(400, "优惠券尚未开始");
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(400, "优惠券已结束");
        }

        LambdaQueryWrapper<UserCoupon> countWrapper = new LambdaQueryWrapper<UserCoupon>();
        countWrapper.eq(UserCoupon::getUserId, userId).eq(UserCoupon::getPromotionId, promotionId);
        Long claimCount = userCouponMapper.selectCount(countWrapper);
        int claimLimit = activity.getClaimLimitPerUser() == null || activity.getClaimLimitPerUser() <= 0 ? 1 : activity.getClaimLimitPerUser();
        if (claimCount != null && claimCount >= claimLimit) {
            throw new BusinessException(400, "已达到该活动领取上限");
        }

        int total = coupon.getTotal() == null ? 0 : coupon.getTotal();
        int claimed = coupon.getClaimed() == null ? 0 : coupon.getClaimed();
        if (total > 0 && claimed >= total) {
            throw new BusinessException(400, "优惠券已领完");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setPromotionId(promotionId);
        userCoupon.setStatus("unused");
        userCoupon.setCreatedAt(now);
        userCoupon.setUpdatedAt(now);
        userCouponMapper.insert(userCoupon);

        LambdaUpdateWrapper<Coupon> updateWrapper = new LambdaUpdateWrapper<Coupon>();
        updateWrapper.eq(Coupon::getId, coupon.getId()).set(Coupon::getUpdatedAt, now).setSql("claimed = COALESCE(claimed, 0) + 1");
        if (total > 0) {
            updateWrapper.lt(Coupon::getClaimed, total);
        }
        int updated = couponMapper.update(null, updateWrapper);
        if (updated <= 0) {
            throw new BusinessException(400, "优惠券已领完");
        }

        return Result.success("领取成功", null);
    }

    private String resolveClaimStatus(PromotionActivity activity, Coupon coupon, Long userClaimCount) {
        int claimLimit = activity.getClaimLimitPerUser() == null || activity.getClaimLimitPerUser() <= 0 ? 1 : activity.getClaimLimitPerUser();
        if (userClaimCount != null && userClaimCount >= claimLimit) {
            return "claimed";
        }
        int total = coupon.getTotal() == null ? 0 : coupon.getTotal();
        int claimed = coupon.getClaimed() == null ? 0 : coupon.getClaimed();
        if (total > 0 && claimed >= total) {
            return "sold_out";
        }
        return "available";
    }
}
