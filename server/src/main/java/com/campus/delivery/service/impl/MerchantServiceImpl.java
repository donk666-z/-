package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.mapper.MerchantMapper;
import com.campus.delivery.service.MerchantService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    @Override
    public List<Merchant> getList(String keyword, String status) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Merchant::getName, keyword);
        }
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Merchant::getStatus, status);
        }
        
        return list(wrapper);
    }

    @Override
    public Merchant getDetail(Long id) {
        return getById(id);
    }

    @Override
    public Merchant getByUserId(Long userId) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, userId);
        return getOne(wrapper);
    }
}
