package com.campus.delivery.controller.rider;

import com.campus.delivery.common.Result;
import com.campus.delivery.service.RiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/rider/location")
public class RiderLocationController {

    @Autowired
    private RiderService riderService;

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        Double latitude = Double.parseDouble(params.get("latitude").toString());
        Double longitude = Double.parseDouble(params.get("longitude").toString());
        Long orderId = params.get("orderId") != null ? Long.parseLong(params.get("orderId").toString()) : null;
        riderService.updateLocation(riderId, latitude, longitude, orderId);
        return Result.success("位置更新成功", null);
    }
}
