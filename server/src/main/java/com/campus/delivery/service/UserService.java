package com.campus.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.delivery.dto.AddressSaveResult;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    User getSafeProfile(Long userId);

    User updateStudentProfile(Long userId, User user);

    List<Address> getAddresses(Long userId);

    AddressSaveResult addAddress(Address address);

    AddressSaveResult updateAddress(Address address);

    void deleteAddress(Long id, Long userId);

    void setDefaultAddress(Long userId, Long addressId);
}
