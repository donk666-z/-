package com.campus.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.delivery.entity.Merchant;
import java.util.List;

public interface MerchantService extends IService<Merchant> {

    List<Merchant> getList(String keyword, String status);

    Merchant getDetail(Long id);

    Merchant getByUserId(Long userId);
}
