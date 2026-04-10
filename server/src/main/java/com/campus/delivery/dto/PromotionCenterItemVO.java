package com.campus.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionCenterItemVO implements Serializable {

    private Long promotionId;

    private String promotionName;

    private Long couponId;

    private String couponName;

    private String type;

    private BigDecimal discount;

    private BigDecimal minAmount;

    private LocalDateTime endTime;

    private String claimStatus;

    private String description;
}
