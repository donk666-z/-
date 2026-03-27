package com.campus.delivery.controller.rider;

import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Order;
import com.campus.delivery.service.OrderService;
import com.campus.delivery.service.RiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/rider/task")
public class RiderTaskController {

    @Autowired
    private RiderService riderService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/available")
    public Result<List<Order>> getAvailable() {
        List<Order> orders = riderService.getAvailableOrders();
        return Result.success(orders);
    }

    @PostMapping("/{id}/grab")
    public Result<Void> grab(@PathVariable Long id, HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        riderService.grabOrder(id, riderId);
        return Result.success("抢单成功", null);
    }

    @PostMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Long id, HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        orderService.pickupOrder(id, riderId);
        return Result.success("已开始配送", null);
    }

    @PostMapping("/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id) {
        orderService.deliverOrder(id);
        return Result.success("已送达", null);
    }

    @GetMapping("/current")
    public Result<List<Order>> getCurrent(HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        List<Order> orders = orderService.getOrdersByRider(riderId, "delivering");
        return Result.success(orders);
    }

    @GetMapping("/history")
    public Result<List<Order>> getHistory(HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        List<Order> orders = orderService.getOrdersByRider(riderId, "completed");
        return Result.success(orders);
    }
}
