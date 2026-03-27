package com.campus.delivery.controller.merchant;

import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/merchant/shop")
public class MerchantShopController {

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/info")
    public Result<Merchant> getInfo(HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        return Result.success(merchant);
    }

    @PutMapping("/info")
    public Result<Merchant> update(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Merchant existing = requireMerchant(request);

        Merchant merchant = new Merchant();
        merchant.setId(existing.getId());
        if (params.containsKey("name")) {
            merchant.setName(normalizeText(params.get("name")));
        }
        if (params.containsKey("description")) {
            merchant.setDescription(normalizeText(params.get("description")));
        }
        if (params.containsKey("phone")) {
            merchant.setPhone(normalizeText(params.get("phone")));
        }
        if (params.containsKey("businessHours")) {
            merchant.setBusinessHours(normalizeText(params.get("businessHours")));
        }
        if (params.containsKey("address")) {
            merchant.setAddress(normalizeText(params.get("address")));
        }
        if (params.containsKey("status")) {
            merchant.setStatus(normalizeStatus(params.get("status"), existing.getStatus()));
        }

        merchantService.updateById(merchant);
        Merchant latest = merchantService.getById(existing.getId());
        return Result.success("更新成功", latest);
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Merchant existing = requireMerchant(request);
        Merchant merchant = new Merchant();
        merchant.setId(existing.getId());
        merchant.setStatus(normalizeStatus(params.get("status"), existing.getStatus()));
        merchantService.updateById(merchant);
        return Result.success("状态更新成功", null);
    }

    private Merchant requireMerchant(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getByUserId(userId);
        if (merchant == null) {
            throw new BusinessException(404, "商户不存在");
        }
        return merchant;
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private String normalizeStatus(Object status, String fallback) {
        if (status == null) {
            return fallback;
        }
        String value = String.valueOf(status).trim().toLowerCase();
        if ("open".equals(value) || "closed".equals(value) || "busy".equals(value)) {
            return value;
        }
        return fallback;
    }
}
