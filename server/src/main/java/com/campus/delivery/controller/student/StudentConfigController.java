package com.campus.delivery.controller.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.SystemConfig;
import com.campus.delivery.mapper.SystemConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/config")
public class StudentConfigController {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @GetMapping("/delivery-fee")
    public Result<String> getDeliveryFee() {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<SystemConfig>();
        wrapper.eq(SystemConfig::getConfigKey, "delivery_fee");
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        String value = "3.00";
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            value = config.getConfigValue().trim();
        }
        return Result.success(value);
    }
}
