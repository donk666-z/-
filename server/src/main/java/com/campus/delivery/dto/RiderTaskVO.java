package com.campus.delivery.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RiderTaskVO implements Serializable {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long riderId;

    private String merchantName;
    private String merchantAddress;
    private Double merchantLatitude;
    private Double merchantLongitude;

    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;

    private BigDecimal deliveryFee;
    private BigDecimal totalPrice;
    private Double distance;
    private String remark;
    private String status;
    private String estimatedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;
}
