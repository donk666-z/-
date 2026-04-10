package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Rider;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.RiderMapper;
import com.campus.delivery.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/rider")
public class AdminRiderController {

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> getList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accountStatus
    ) {
        Map<Long, User> riderUserMap = loadRiderUserMap();
        if (riderUserMap.isEmpty()) {
            return Result.success(new Page<>(page, size, 0));
        }

        LambdaQueryWrapper<Rider> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Rider::getId, riderUserMap.keySet());
        if (StringUtils.hasText(status)) {
            wrapper.eq(Rider::getStatus, status.trim());
        }
        wrapper.orderByDesc(Rider::getUpdatedAt).orderByDesc(Rider::getId);

        List<Map<String, Object>> records = new ArrayList<>();
        String keywordValue = normalize(keyword);
        String accountStatusValue = normalize(accountStatus);

        for (Rider rider : riderMapper.selectList(wrapper)) {
            User user = riderUserMap.get(rider.getId());
            if (user == null) {
                continue;
            }
            if (StringUtils.hasText(accountStatusValue) && !accountStatusValue.equals(normalize(user.getStatus()))) {
                continue;
            }
            if (StringUtils.hasText(keywordValue) && !matchesKeyword(rider, user, keywordValue)) {
                continue;
            }
            records.add(buildRecord(rider, user));
        }

        long total = records.size();
        int start = Math.max(0, (page - 1) * size);
        int end = Math.min((int) total, start + size);

        Page<Map<String, Object>> result = new Page<>(page, size, total);
        result.setRecords(start >= end ? Collections.emptyList() : records.subList(start, end));
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String status = params == null ? null : params.get("status");
        String normalizedStatus = normalize(status);
        if (!StringUtils.hasText(normalizedStatus)) {
            return Result.error(400, "骑手状态不能为空");
        }
        if (!"online".equals(normalizedStatus) && !"offline".equals(normalizedStatus) && !"delivering".equals(normalizedStatus)) {
            return Result.error(400, "骑手状态不合法");
        }

        Rider rider = riderMapper.selectById(id);
        if (rider == null) {
            return Result.error(404, "骑手不存在");
        }

        Rider update = new Rider();
        update.setId(id);
        update.setStatus(normalizedStatus);
        riderMapper.updateById(update);
        return Result.success("骑手状态更新成功", null);
    }

    private Map<Long, User> loadRiderUserMap() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "rider");
        List<User> users = userMapper.selectList(wrapper);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(User::getId, user -> user));
    }

    private Map<String, Object> buildRecord(Rider rider, User user) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", rider.getId());
        item.put("name", StringUtils.hasText(rider.getName()) ? rider.getName() : user.getNickname());
        item.put("nickname", user.getNickname());
        item.put("phone", StringUtils.hasText(rider.getPhone()) ? rider.getPhone() : user.getPhone());
        item.put("avatar", StringUtils.hasText(rider.getAvatar()) ? rider.getAvatar() : user.getAvatar());
        item.put("status", rider.getStatus());
        item.put("accountStatus", user.getStatus());
        item.put("totalOrders", rider.getTotalOrders() == null ? 0 : rider.getTotalOrders());
        item.put("totalIncome", rider.getTotalIncome());
        item.put("createdAt", rider.getCreatedAt());
        item.put("updatedAt", rider.getUpdatedAt());
        return item;
    }

    private boolean matchesKeyword(Rider rider, User user, String keyword) {
        return contains(rider.getName(), keyword)
                || contains(rider.getPhone(), keyword)
                || contains(user.getNickname(), keyword)
                || contains(user.getPhone(), keyword);
    }

    private boolean contains(String source, String keyword) {
        return StringUtils.hasText(source) && source.toLowerCase().contains(keyword);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
