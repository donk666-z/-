package com.campus.delivery.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RouteStepVO implements Serializable {

    private String instruction;
    private String roadName;
    private String direction;
    private String action;
    private Integer distanceMeters;
}
