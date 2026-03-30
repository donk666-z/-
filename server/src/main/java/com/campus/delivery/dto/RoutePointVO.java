package com.campus.delivery.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RoutePointVO implements Serializable {

    private Double latitude;
    private Double longitude;
}
