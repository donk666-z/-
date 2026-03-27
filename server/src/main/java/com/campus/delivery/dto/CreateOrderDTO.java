package com.campus.delivery.dto;

import com.campus.delivery.model.ComboSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderDTO {
    
    private Long merchantId;
    
    private List<OrderItemDTO> items;
    
    private String address;
    
    private String phone;
    
    private String remark;
    
    private BigDecimal deliveryFee;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemDTO {
        private Long dishId;
        private String type;
        private String name;
        private String image;
        private BigDecimal price;
        private Integer quantity;
        private List<ComboSelectionGroupDTO> comboSelections;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComboSelectionGroupDTO {
        private Integer groupIndex;
        private String name;
        private List<ComboSelectionOptionDTO> options;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComboSelectionOptionDTO {
        private Long dishId;
        private String dishName;
        private Integer quantity;
        private BigDecimal extraPrice;
    }
}
