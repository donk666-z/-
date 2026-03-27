package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("riders")
public class Rider implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String openid;
    
    private String name;
    
    private String phone;
    
    private String avatar;
    
    private String status;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private Integer totalOrders;
    
    private BigDecimal totalIncome;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
