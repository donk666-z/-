package com.campus.delivery.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.campus.delivery.service.GeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TencentGeocodingServiceImpl implements GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(TencentGeocodingServiceImpl.class);

    @Value("${tencent.map.key:}")
    private String tencentMapKey;

    @Override
    public Coordinate resolve(String address, String scene) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        if (!StringUtils.hasText(tencentMapKey)) {
            log.warn("Geocoding skipped because tencent.map.key is blank. scene={}, address={}", scene, address);
            return null;
        }

        try {
            String response = HttpUtil.createGet("https://apis.map.qq.com/ws/geocoder/v1/")
                    .form("address", address.trim())
                    .form("output", "json")
                    .form("key", tencentMapKey)
                    .timeout(5000)
                    .execute()
                    .body();

            JSONObject body = JSONUtil.parseObj(response);
            if (body.getInt("status", -1) != 0) {
                log.warn("Geocoding failed. scene={}, address={}, message={}", scene, address, body.getStr("message", "未知错误"));
                return null;
            }

            JSONObject result = body.getJSONObject("result");
            JSONObject location = result == null ? null : result.getJSONObject("location");
            if (location == null) {
                log.warn("Geocoding returned empty location. scene={}, address={}", scene, address);
                return null;
            }

            Double latitude = location.getDouble("lat");
            Double longitude = location.getDouble("lng");
            if (latitude == null || longitude == null) {
                log.warn("Geocoding returned invalid coordinates. scene={}, address={}", scene, address);
                return null;
            }

            return new Coordinate(roundCoordinate(latitude), roundCoordinate(longitude));
        } catch (Exception ex) {
            log.warn("Geocoding exception. scene={}, address={}, error={}", scene, address, ex.getMessage());
            return null;
        }
    }

    private BigDecimal roundCoordinate(Double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }
}
