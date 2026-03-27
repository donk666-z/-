package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("reviews")
public class Review implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long orderId;
    
    private Long userId;
    
    private Long merchantId;
    
    private Integer foodRating;
    
    private Integer deliveryRating;
    
    private String content;
    
    private String images;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
