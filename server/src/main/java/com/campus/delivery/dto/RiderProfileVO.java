package com.campus.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RiderProfileVO {
    private Long id;
    private String name;
    private String phone;
    private String avatar;
    private String status;
    private Integer totalOrders;
    private BigDecimal totalIncome;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
