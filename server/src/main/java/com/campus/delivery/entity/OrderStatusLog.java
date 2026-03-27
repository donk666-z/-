package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("order_status_logs")
public class OrderStatusLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String status;

    private String operator;

    private String remark;

    private LocalDateTime createdAt;
}
