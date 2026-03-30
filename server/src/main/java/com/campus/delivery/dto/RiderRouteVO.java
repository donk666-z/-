package com.campus.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class RiderRouteVO implements Serializable {

    private Boolean available;
    private Boolean smart;
    private String provider;
    private String mode;
    private String sourceType;
    private String sourceLabel;
    private String destinationLabel;
    private Integer distanceMeters;
    private Integer durationMinutes;
    private String etaText;
    private String note;
    private List<RoutePointVO> points = new ArrayList<>();
    private List<RouteStepVO> steps = new ArrayList<>();
}
