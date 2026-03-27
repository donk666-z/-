package com.campus.delivery.controller.student;

import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.User;
import com.campus.delivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/student/user")
public class StudentUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public Result<User> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody User user, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        user.setId(userId);
        userService.updateById(user);
        return Result.success("更新成功", null);
    }

    @GetMapping("/addresses")
    public Result<List<Address>> getAddresses(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Address> addresses = userService.getAddresses(userId);
        return Result.success(addresses);
    }

    @PostMapping("/addresses")
    public Result<Address> addAddress(@RequestBody Address address, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            // 避免出现 SQL 层 user_id 缺失导致的难以定位错误
            throw new com.campus.delivery.exception.BusinessException(401, "未授权，请先登录");
        }
        address.setUserId(userId);
        Address result = userService.addAddress(address);
        return Result.success("地址添加成功", result);
    }

    @PutMapping("/addresses/{id}")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody Address address, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        address.setId(id);
        address.setUserId(userId);
        userService.updateAddress(address);
        return Result.success("地址更新成功", null);
    }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.deleteAddress(id, userId);
        return Result.success("地址删除成功", null);
    }

    @PutMapping("/addresses/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.setDefaultAddress(userId, id);
        return Result.success("已设为默认地址", null);
    }
}
