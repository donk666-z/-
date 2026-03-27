package com.campus.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("complaint")
public class Complaint implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    private String content;

    private String contact;

    private String status;

    private String reply;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
