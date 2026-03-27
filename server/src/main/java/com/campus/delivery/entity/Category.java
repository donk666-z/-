package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("categories")
public class Category implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long merchantId;
    
    private String name;
    
    private Integer sort;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
