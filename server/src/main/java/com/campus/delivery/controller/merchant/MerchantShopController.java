package com.campus.delivery.controller.merchant;

import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.service.GeocodingService;
import com.campus.delivery.service.MerchantService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/merchant/shop")
public class MerchantShopController {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private GeocodingService geocodingService;

    @GetMapping("/info")
    public Result<Merchant> getInfo(HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        return Result.success(merchant);
    }

    @PutMapping("/info")
    public Result<Merchant> update(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Merchant existing = requireMerchant(request);
        boolean geocodeAttempted = false;
        boolean geocodeResolved = false;

        LambdaUpdateWrapper<Merchant> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Merchant::getId, existing.getId());
        if (params.containsKey("name")) {
            updateWrapper.set(Merchant::getName, normalizeText(params.get("name")));
        }
        if (params.containsKey("logo")) {
            updateWrapper.set(Merchant::getLogo, normalizeText(params.get("logo")));
        }
        if (params.containsKey("description")) {
            updateWrapper.set(Merchant::getDescription, normalizeText(params.get("description")));
        }
        if (params.containsKey("phone")) {
            updateWrapper.set(Merchant::getPhone, normalizeText(params.get("phone")));
        }
        if (params.containsKey("businessHours")) {
            updateWrapper.set(Merchant::getBusinessHours, normalizeText(params.get("businessHours")));
        }
        if (params.containsKey("address")) {
            String address = normalizeText(params.get("address"));
            updateWrapper.set(Merchant::getAddress, address);
            if (StringUtils.hasText(address)) {
                geocodeAttempted = true;
                GeocodingService.Coordinate coordinate = geocodingService.resolve(address, "merchant-shop");
                if (coordinate != null) {
                    geocodeResolved = true;
                    updateWrapper.set(Merchant::getLatitude, coordinate.getLatitude());
                    updateWrapper.set(Merchant::getLongitude, coordinate.getLongitude());
                } else {
                    updateWrapper.set(Merchant::getLatitude, null);
                    updateWrapper.set(Merchant::getLongitude, null);
                }
            } else {
                updateWrapper.set(Merchant::getLatitude, null);
                updateWrapper.set(Merchant::getLongitude, null);
            }
        }
        if (params.containsKey("status")) {
            updateWrapper.set(Merchant::getStatus, normalizeStatus(params.get("status"), existing.getStatus()));
        }

        merchantService.update(updateWrapper);
        Merchant latest = merchantService.getById(existing.getId());
        return Result.success(buildShopSaveMessage(geocodeAttempted, geocodeResolved), latest);
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

    private String buildShopSaveMessage(boolean geocodeAttempted, boolean geocodeResolved) {
        if (geocodeAttempted && !geocodeResolved) {
            return "更新成功，但未能解析出经纬度";
        }
        return "更新成功";
    }
}
