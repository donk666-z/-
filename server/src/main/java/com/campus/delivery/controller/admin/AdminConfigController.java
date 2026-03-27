package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.SystemConfig;
import com.campus.delivery.mapper.SystemConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @GetMapping("/list")
    public Result<List<SystemConfig>> getAll() {
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        return Result.success(configs);
    }

    @PutMapping("/{key}")
    public Result<Void> update(@PathVariable String key, @RequestBody SystemConfig config) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        config.setConfigKey(key);
        systemConfigMapper.update(config, wrapper);
        return Result.success("配置更新成功", null);
    }
}
