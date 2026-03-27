package com.campus.delivery.service;

import com.campus.delivery.entity.User;

public interface AuthService {
    
    User wxLogin(String code);
    
    String generateToken(User user);
    
    User getUserByToken(String token);
}
