package com.campus.delivery.controller.rider;

import com.campus.delivery.common.Result;
import com.campus.delivery.service.RiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/rider/stats")
public class RiderStatsController {

    @Autowired
    private RiderService riderService;

    @GetMapping
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = riderService.getStats(riderId);
        return Result.success(stats);
    }
}
