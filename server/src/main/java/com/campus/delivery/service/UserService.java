package com.campus.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.User;
import java.util.List;

public interface UserService extends IService<User> {
    
    List<Address> getAddresses(Long userId);
    
    Address addAddress(Address address);
    
    void updateAddress(Address address);
    
    void deleteAddress(Long id, Long userId);

    void setDefaultAddress(Long userId, Long addressId);
}
