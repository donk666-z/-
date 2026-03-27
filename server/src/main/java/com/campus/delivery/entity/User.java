package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String openid;
    
    private String nickname;
    
    private String avatar;
    
    private String phone;

    private String password;

    private String role;
    
    private String status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
