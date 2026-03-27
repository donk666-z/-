package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.delivery.model.ComboSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_items")
public class OrderItem implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long orderId;
    
    private Long dishId;
    
    private String dishName;
    
    private String dishImage;
    
    private BigDecimal price;
    
    private Integer quantity;

    @TableField("item_type")
    private String itemType;

    @JsonIgnore
    @TableField("combo_snapshot")
    private String comboSnapshotJson;

    @TableField(exist = false)
    private ComboSnapshot comboSnapshot;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
