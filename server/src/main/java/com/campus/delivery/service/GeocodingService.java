package com.campus.delivery.service;

import java.math.BigDecimal;

public interface GeocodingService {

    Coordinate resolve(String address, String scene);

    class Coordinate {
        private final BigDecimal latitude;
        private final BigDecimal longitude;

        public Coordinate(BigDecimal latitude, BigDecimal longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public BigDecimal getLatitude() {
            return latitude;
        }

        public BigDecimal getLongitude() {
            return longitude;
        }
    }
}
