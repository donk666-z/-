package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchants")
public class Merchant implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;
    
    private String logo;
    
    private String description;
    
    private String address;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private String phone;
    
    private String businessHours;
    
    private String status;
    
    private Integer monthSales;
    
    private BigDecimal rating;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
