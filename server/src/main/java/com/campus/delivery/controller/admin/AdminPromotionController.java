package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Coupon;
import com.campus.delivery.entity.PromotionActivity;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.CouponMapper;
import com.campus.delivery.mapper.PromotionActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/promotion")
public class AdminPromotionController {

    @Autowired
    private PromotionActivityMapper promotionActivityMapper;

    @Autowired
    private CouponMapper couponMapper;

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> getList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        LambdaQueryWrapper<PromotionActivity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PromotionActivity::getName, keyword.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PromotionActivity::getStatus, status.trim());
        }
        wrapper.orderByDesc(PromotionActivity::getCreatedAt);

        Page<PromotionActivity> pageObj = new Page<PromotionActivity>(page, size);
        promotionActivityMapper.selectPage(pageObj, wrapper);

        Page<Map<String, Object>> result = new Page<Map<String, Object>>(pageObj.getCurrent(), pageObj.getSize(), pageObj.getTotal());
        result.setRecords(toRecords(pageObj.getRecords()));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        PromotionActivity activity = promotionActivityMapper.selectById(id);
        if (activity == null) {
            return Result.error(404, "促销活动不存在");
        }
        List<PromotionActivity> activities = new ArrayList<PromotionActivity>();
        activities.add(activity);
        List<Map<String, Object>> records = toRecords(activities);
        return Result.success(records.isEmpty() ? null : records.get(0));
    }

    @PostMapping
    public Result<Void> create(@RequestBody PromotionActivity activity) {
        validateActivity(activity, null);
        LocalDateTime now = LocalDateTime.now();
        activity.setStatus(normalizeStatus(activity.getStatus(), "draft"));
        activity.setClaimLimitPerUser(normalizeClaimLimit(activity.getClaimLimitPerUser()));
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        promotionActivityMapper.insert(activity);
        return Result.success("促销活动创建成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PromotionActivity activity) {
        PromotionActivity existing = promotionActivityMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "促销活动不存在");
        }

        validateActivity(activity, existing);
        activity.setId(id);
        activity.setStatus(normalizeStatus(activity.getStatus(), existing.getStatus()));
        activity.setClaimLimitPerUser(normalizeClaimLimit(activity.getClaimLimitPerUser()));
        activity.setCreatedAt(existing.getCreatedAt());
        activity.setUpdatedAt(LocalDateTime.now());
        promotionActivityMapper.updateById(activity);
        return Result.success("促销活动更新成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        PromotionActivity existing = promotionActivityMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "促销活动不存在");
        }

        String normalizedStatus = normalizeStatus(body == null ? null : body.get("status"), null);
        if (!StringUtils.hasText(normalizedStatus)) {
            return Result.error(400, "活动状态不能为空");
        }

        PromotionActivity update = new PromotionActivity();
        update.setId(id);
        update.setStatus(normalizedStatus);
        update.setUpdatedAt(LocalDateTime.now());
        promotionActivityMapper.updateById(update);
        return Result.success("活动状态更新成功", null);
    }

    private void validateActivity(PromotionActivity activity, PromotionActivity existing) {
        if (activity == null) {
            throw new BusinessException(400, "促销活动参数不能为空");
        }

        String name = StringUtils.hasText(activity.getName()) ? activity.getName().trim() : (existing == null ? null : existing.getName());
        Long couponId = activity.getCouponId() != null ? activity.getCouponId() : (existing == null ? null : existing.getCouponId());
        LocalDateTime startTime = activity.getStartTime() != null ? activity.getStartTime() : (existing == null ? null : existing.getStartTime());
        LocalDateTime endTime = activity.getEndTime() != null ? activity.getEndTime() : (existing == null ? null : existing.getEndTime());

        if (!StringUtils.hasText(name)) {
            throw new BusinessException(400, "活动名称不能为空");
        }
        if (couponId == null) {
            throw new BusinessException(400, "请选择优惠券");
        }

        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(404, "关联优惠券不存在");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException(400, "请完整填写活动时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(400, "活动结束时间必须晚于开始时间");
        }
        if (activity.getClaimLimitPerUser() != null && activity.getClaimLimitPerUser() <= 0) {
            throw new BusinessException(400, "每人领取上限必须大于 0");
        }
        activity.setName(name);
        activity.setCouponId(couponId);
    }

    private List<Map<String, Object>> toRecords(List<PromotionActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> couponIds = activities.stream()
                .map(PromotionActivity::getCouponId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, Coupon> couponMap;
        if (couponIds.isEmpty()) {
            couponMap = Collections.emptyMap();
        } else {
            couponMap = couponMapper.selectBatchIds(couponIds).stream()
                    .collect(Collectors.toMap(Coupon::getId, item -> item));
        }

        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (PromotionActivity activity : activities) {
            Coupon coupon = couponMap.get(activity.getCouponId());
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", activity.getId());
            item.put("name", activity.getName());
            item.put("couponId", activity.getCouponId());
            item.put("couponName", coupon == null ? "" : coupon.getName());
            item.put("couponType", coupon == null ? "" : coupon.getType());
            item.put("discount", coupon == null ? null : coupon.getDiscount());
            item.put("minAmount", coupon == null ? null : coupon.getMinAmount());
            item.put("status", activity.getStatus());
            item.put("startTime", activity.getStartTime());
            item.put("endTime", activity.getEndTime());
            item.put("claimLimitPerUser", activity.getClaimLimitPerUser());
            item.put("description", activity.getDescription());
            item.put("createdAt", activity.getCreatedAt());
            item.put("updatedAt", activity.getUpdatedAt());
            records.add(item);
        }
        return records;
    }

    private String normalizeStatus(String status, String fallback) {
        if (!StringUtils.hasText(status)) {
            return fallback;
        }
        String normalized = status.trim().toLowerCase();
        if (!"draft".equals(normalized) && !"active".equals(normalized) && !"inactive".equals(normalized)) {
            throw new BusinessException(400, "活动状态不合法");
        }
        return normalized;
    }

    private Integer normalizeClaimLimit(Integer claimLimitPerUser) {
        if (claimLimitPerUser == null || claimLimitPerUser <= 0) {
            return 1;
        }
        return claimLimitPerUser;
    }
}
