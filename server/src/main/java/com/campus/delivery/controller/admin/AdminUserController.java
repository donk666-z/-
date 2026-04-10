package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.service.UserService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MerchantMapper merchantMapper;

    @GetMapping("/list")
    public Result<Page<User>> getList(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status
    ) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getNickname, kw).or().like(User::getPhone, kw));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status.trim());
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userService.page(new Page<User>(page, size), wrapper);
        fillMerchantNickname(result.getRecords());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<User> getDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            List<User> users = new ArrayList<User>();
            users.add(user);
            fillMerchantNickname(users);
        }
        return Result.success(user);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String status = params == null ? null : params.get("status");
        if (!StringUtils.hasText(status)) {
            return Result.error(400, "状态不能为空");
        }
        User user = new User();
        user.setId(id);
        user.setStatus(status.trim());
        userService.updateById(user);
        return Result.success("状态更新成功", null);
    }

    private void fillMerchantNickname(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        Set<Long> merchantUserIds = users.stream()
                .filter(user -> user != null && "merchant".equals(user.getRole()))
                .filter(user -> !StringUtils.hasText(user.getNickname()))
                .map(User::getId)
                .collect(Collectors.toSet());
        if (merchantUserIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Merchant::getUserId, merchantUserIds);
        Map<Long, String> merchantNameMap = merchantMapper.selectList(wrapper).stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(Merchant::getUserId, Merchant::getName, (left, right) -> left));

        for (User user : users) {
            if (user == null || !"merchant".equals(user.getRole()) || StringUtils.hasText(user.getNickname())) {
                continue;
            }
            String merchantName = merchantNameMap.get(user.getId());
            if (StringUtils.hasText(merchantName)) {
                user.setNickname(merchantName);
            }
        }
    }
}
