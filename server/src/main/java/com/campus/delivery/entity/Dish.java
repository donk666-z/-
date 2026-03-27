package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.delivery.model.ComboConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dishes")
public class Dish implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long merchantId;
    
    private Long categoryId;
    
    private String name;
    
    private String description;
    
    private String image;
    
    private BigDecimal price;
    
    private Integer stock;
    
    private Integer sales;

    private String status;

    @TableField("dish_type")
    private String type;

    @JsonIgnore
    @TableField("combo_config")
    private String comboConfigJson;

    @TableField(exist = false)
    private String category;

    @TableField(exist = false)
    private ComboConfig comboConfig;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
