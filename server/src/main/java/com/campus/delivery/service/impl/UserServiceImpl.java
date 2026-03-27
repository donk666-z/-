package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.User;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.AddressMapper;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> getAddresses(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .orderByDesc(Address::getIsDefault)
               .orderByDesc(Address::getCreatedAt);
        return addressMapper.selectList(wrapper);
    }

    @Override
    public Address addAddress(Address address) {
        if (address.getIsDefault() != null && address.getIsDefault()) {
            LambdaUpdateWrapper<Address> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Address::getUserId, address.getUserId())
                        .set(Address::getIsDefault, false);
            addressMapper.update(null, updateWrapper);
        }
        
        addressMapper.insert(address);
        return address;
    }

    @Override
    public void updateAddress(Address address) {
        if (address.getIsDefault() != null && address.getIsDefault()) {
            LambdaUpdateWrapper<Address> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Address::getUserId, address.getUserId())
                        .set(Address::getIsDefault, false);
            addressMapper.update(null, updateWrapper);
        }
        
        addressMapper.updateById(address);
    }

    @Override
    public void deleteAddress(Long id, Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getId, id)
               .eq(Address::getUserId, userId);
        addressMapper.delete(wrapper);
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        LambdaQueryWrapper<Address> verify = new LambdaQueryWrapper<>();
        verify.eq(Address::getId, addressId).eq(Address::getUserId, userId);
        Address target = addressMapper.selectOne(verify);
        if (target == null) {
            throw new BusinessException(404, "地址不存在");
        }
        LambdaUpdateWrapper<Address> clear = new LambdaUpdateWrapper<>();
        clear.eq(Address::getUserId, userId).set(Address::getIsDefault, false);
        addressMapper.update(null, clear);
        target.setIsDefault(true);
        addressMapper.updateById(target);
    }
}
