package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("promotion_activities")
public class PromotionActivity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long couponId;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer claimLimitPerUser;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
