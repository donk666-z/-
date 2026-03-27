package com.campus.delivery.controller.admin;

import com.campus.delivery.common.Result;
import com.campus.delivery.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> stats = orderService.getPlatformStats();
        return Result.success(stats);
    }
}
