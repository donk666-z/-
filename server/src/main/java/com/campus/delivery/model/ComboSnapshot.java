package com.campus.delivery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComboSnapshot implements Serializable {

    private List<Rule> rules;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule implements Serializable {
        private Long categoryId;
        private String categoryName;
        private Integer requiredCount;
        private List<Item> items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item implements Serializable {
        private Long dishId;
        private String dishName;
        private String dishImage;
        private Integer quantity;
        private Integer totalQuantity;
        private BigDecimal extraPrice;
        private BigDecimal dishPrice;
    }
}
