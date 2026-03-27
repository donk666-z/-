package com.campus.delivery.controller.merchant;

import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.service.MerchantService;
import com.campus.delivery.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/merchant/stats")
public class MerchantStatsController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getByUserId(userId);
        Map<String, Object> stats = orderService.getMerchantStats(merchant.getId());
        return Result.success(stats);
    }
}
