package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo;
    
    private Long userId;
    
    private Long merchantId;
    
    private Long riderId;
    
    private BigDecimal dishPrice;
    
    private BigDecimal deliveryFee;
    
    private BigDecimal totalPrice;
    
    private String address;
    
    private String phone;
    
    private String remark;
    
    private String status;
    
    private Integer estimatedTime;
    
    private String paymentMethod;
    
    private LocalDateTime paymentTime;
    
    private LocalDateTime completedTime;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
