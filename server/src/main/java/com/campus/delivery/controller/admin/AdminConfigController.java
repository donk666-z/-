package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.SystemConfig;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.SystemConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @GetMapping("/list")
    public Result<List<SystemConfig>> getAll() {
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        Map<String, SystemConfig> configMap = new LinkedHashMap<String, SystemConfig>();
        for (SystemConfig config : configs) {
            configMap.put(config.getConfigKey(), config);
        }

        List<SystemConfig> result = new ArrayList<SystemConfig>();
        result.add(mergeDefault("delivery_fee", "3.00", "默认配送费（元）", configMap.get("delivery_fee")));
        result.add(mergeDefault("min_order_amount", "10.00", "最低起送金额（元）", configMap.get("min_order_amount")));
        result.add(mergeDefault("platform_commission", "0.15", "平台抽成比例（0-1）", configMap.get("platform_commission")));

        for (SystemConfig config : configs) {
            if ("delivery_fee".equals(config.getConfigKey())
                    || "min_order_amount".equals(config.getConfigKey())
                    || "platform_commission".equals(config.getConfigKey())) {
                continue;
            }
            result.add(config);
        }
        return Result.success(result);
    }

    @PutMapping("/{key}")
    public Result<Void> update(@PathVariable String key, @RequestBody SystemConfig config) {
        String configValue = config == null ? null : config.getConfigValue();
        validateConfig(key, configValue);

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<SystemConfig>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig existing = systemConfigMapper.selectOne(wrapper);
        if (existing == null) {
            SystemConfig created = new SystemConfig();
            created.setConfigKey(key);
            created.setConfigValue(configValue);
            created.setDescription(config == null ? null : config.getDescription());
            created.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.insert(created);
        } else {
            SystemConfig updated = new SystemConfig();
            updated.setId(existing.getId());
            updated.setConfigKey(key);
            updated.setConfigValue(configValue);
            updated.setDescription(config == null || !StringUtils.hasText(config.getDescription()) ? existing.getDescription() : config.getDescription());
            updated.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.updateById(updated);
        }
        return Result.success("配置更新成功", null);
    }

    private SystemConfig mergeDefault(String key, String defaultValue, String description, SystemConfig existing) {
        if (existing != null) {
            if (!StringUtils.hasText(existing.getConfigValue())) {
                existing.setConfigValue(defaultValue);
            }
            if (!StringUtils.hasText(existing.getDescription())) {
                existing.setDescription(description);
            }
            return existing;
        }

        SystemConfig item = new SystemConfig();
        item.setConfigKey(key);
        item.setConfigValue(defaultValue);
        item.setDescription(description);
        return item;
    }

    private void validateConfig(String key, String value) {
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(400, "配置键不能为空");
        }
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, "配置值不能为空");
        }

        if ("delivery_fee".equals(key) || "min_order_amount".equals(key)) {
            BigDecimal amount = parseDecimal(value, "金额配置格式不正确");
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(400, "金额配置不能小于 0");
            }
            return;
        }

        if ("platform_commission".equals(key)) {
            BigDecimal ratio = parseDecimal(value, "抽成比例格式不正确");
            if (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException(400, "抽成比例必须在 0 到 1 之间");
            }
        }
    }

    private BigDecimal parseDecimal(String value, String message) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            throw new BusinessException(400, message);
        }
    }
}
