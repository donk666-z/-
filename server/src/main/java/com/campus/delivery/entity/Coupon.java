package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupons")
public class Coupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private BigDecimal discount;

    private BigDecimal minAmount;

    private Integer total;

    private Integer claimed;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
