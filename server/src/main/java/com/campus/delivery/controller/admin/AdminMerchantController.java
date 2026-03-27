package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/merchant")
public class AdminMerchantController {

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/list")
    public Result<Page<Merchant>> getList(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status
    ) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Merchant::getName, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Merchant::getStatus, status);
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);
        Page<Merchant> result = merchantService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setStatus(params.get("status"));
        merchantService.updateById(merchant);
        return Result.success("状态更新成功", null);
    }
}
