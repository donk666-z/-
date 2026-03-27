package com.campus.delivery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComboSnapshot implements Serializable {

    private List<Group> groups;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Group implements Serializable {
        private Integer groupIndex;
        private String name;
        private Integer minSelect;
        private Integer maxSelect;
        private List<Option> options;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Option implements Serializable {
        private Long dishId;
        private String dishName;
        private String dishImage;
        private Integer quantity;
        private BigDecimal extraPrice;
        private BigDecimal dishPrice;
    }
}
