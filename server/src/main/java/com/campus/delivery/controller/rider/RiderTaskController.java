package com.campus.delivery.controller.rider;

import com.campus.delivery.common.Result;
import com.campus.delivery.dto.RiderTaskVO;
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
    public Result<List<RiderTaskVO>> getAvailable(HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        List<RiderTaskVO> orders = riderService.getAvailableOrders(riderId);
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
        return Result.success("已确认取餐", null);
    }

    @PostMapping("/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id, HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        orderService.deliverOrder(id, riderId);
        return Result.success("已确认送达", null);
    }

    @GetMapping("/current")
    public Result<RiderTaskVO> getCurrent(HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        RiderTaskVO order = riderService.getCurrentTask(riderId);
        return Result.success(order);
    }

    @GetMapping("/history")
    public Result<List<RiderTaskVO>> getHistory(HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        List<RiderTaskVO> orders = riderService.getHistoryOrders(riderId);
        return Result.success(orders);
    }

    @GetMapping("/{id}")
    public Result<RiderTaskVO> getDetail(@PathVariable Long id, HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        RiderTaskVO task = riderService.getTaskDetail(id, riderId);
        return Result.success(task);
    }
}
