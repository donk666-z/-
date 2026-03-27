package com.campus.delivery.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentOrderVO implements Serializable {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private String merchantName;
    private Long riderId;

    private BigDecimal dishPrice;
    private BigDecimal deliveryFee;
    private BigDecimal totalPrice;

    private String address;
    private String phone;
    private String remark;
    private String status;

    /** 展示用：预计送达文案（如「约30分钟」） */
    private String estimatedTime;

    private String paymentMethod;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String riderName;
    private String riderPhone;
    private Double riderLat;
    private Double riderLng;

    private Double addressLat;
    private Double addressLng;

    private Boolean reviewed;

    private List<OrderDishVO> dishes;

    @Data
    public static class OrderDishVO implements Serializable {
        private Long id;
        private String name;
        private String image;
        private String type;
        private BigDecimal price;
        private Integer quantity;
        private List<ComboGroupVO> comboGroups;
    }

    @Data
    public static class ComboGroupVO implements Serializable {
        private Integer groupIndex;
        private String name;
        private List<ComboOptionVO> options;
    }

    @Data
    public static class ComboOptionVO implements Serializable {
        private Long dishId;
        private String dishName;
        private String dishImage;
        private Integer quantity;
        private Integer totalQuantity;
        private BigDecimal extraPrice;
    }
}
