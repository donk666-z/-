package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.dto.AddressSaveResult;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.User;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.AddressMapper;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.GeocodingService;
import com.campus.delivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private GeocodingService geocodingService;

    @Override
    public User getSafeProfile(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User updateStudentProfile(Long userId, User user) {
        User currentUser = getById(userId);
        if (currentUser == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String nickname = user == null || user.getNickname() == null ? "" : user.getNickname().trim();
        String phone = user == null || user.getPhone() == null ? "" : user.getPhone().trim();
        String avatar = user == null || user.getAvatar() == null ? "" : user.getAvatar().trim();

        if (nickname.isEmpty()) {
            throw new BusinessException(400, "昵称不能为空");
        }
        if (phone.isEmpty()) {
            throw new BusinessException(400, "手机号不能为空");
        }
        if (!phone.matches(PHONE_PATTERN)) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, phone).ne(User::getId, userId);
        if (baseMapper.selectCount(phoneWrapper) > 0) {
            throw new BusinessException(400, "该手机号已注册");
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getNickname, nickname)
                .set(User::getPhone, phone)
                .set(User::getAvatar, avatar);
        update(updateWrapper);

        return getSafeProfile(userId);
    }

    @Override
    public List<Address> getAddresses(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt);
        return addressMapper.selectList(wrapper);
    }

    @Override
    public AddressSaveResult addAddress(Address address) {
        if (address.getIsDefault() != null && address.getIsDefault()) {
            LambdaUpdateWrapper<Address> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Address::getUserId, address.getUserId())
                    .set(Address::getIsDefault, false);
            addressMapper.update(null, updateWrapper);
        }

        GeocodeStatus geocodeStatus = applyCoordinates(address);
        addressMapper.insert(address);
        return new AddressSaveResult(address, geocodeStatus.attempted, geocodeStatus.resolved);
    }

    @Override
    public AddressSaveResult updateAddress(Address address) {
        if (address.getIsDefault() != null && address.getIsDefault()) {
            LambdaUpdateWrapper<Address> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Address::getUserId, address.getUserId())
                    .set(Address::getIsDefault, false);
            addressMapper.update(null, updateWrapper);
        }

        GeocodeStatus geocodeStatus = applyCoordinates(address);
        LambdaUpdateWrapper<Address> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Address::getId, address.getId())
                .eq(Address::getUserId, address.getUserId())
                .set(Address::getName, address.getName())
                .set(Address::getPhone, address.getPhone())
                .set(Address::getAddress, address.getAddress())
                .set(Address::getBuilding, address.getBuilding())
                .set(Address::getRoom, address.getRoom())
                .set(Address::getIsDefault, address.getIsDefault())
                .set(Address::getLatitude, address.getLatitude())
                .set(Address::getLongitude, address.getLongitude());
        addressMapper.update(null, updateWrapper);
        return new AddressSaveResult(address, geocodeStatus.attempted, geocodeStatus.resolved);
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

    private GeocodeStatus applyCoordinates(Address address) {
        String rawAddress = address == null ? null : address.getAddress();
        if (!StringUtils.hasText(rawAddress)) {
            address.setLatitude(null);
            address.setLongitude(null);
            return new GeocodeStatus(false, false);
        }

        GeocodingService.Coordinate coordinate = geocodingService.resolve(rawAddress.trim(), "student-address");
        if (coordinate == null) {
            address.setLatitude(null);
            address.setLongitude(null);
            return new GeocodeStatus(true, false);
        }

        address.setLatitude(coordinate.getLatitude());
        address.setLongitude(coordinate.getLongitude());
        return new GeocodeStatus(true, true);
    }

    private static class GeocodeStatus {
        private final boolean attempted;
        private final boolean resolved;

        private GeocodeStatus(boolean attempted, boolean resolved) {
            this.attempted = attempted;
            this.resolved = resolved;
        }
    }
}
