package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("addresses")
public class Address implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String name;
    
    private String phone;
    
    private String address;
    
    private String building;
    
    private String room;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private Boolean isDefault;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
