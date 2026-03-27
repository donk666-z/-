package com.campus.delivery.controller.student;

import com.campus.delivery.common.Result;
import com.campus.delivery.dto.CreateOrderDTO;
import com.campus.delivery.dto.StudentOrderVO;
import com.campus.delivery.entity.Order;
import com.campus.delivery.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/student/order")
public class StudentOrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@RequestBody CreateOrderDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Order order = orderService.createOrder(dto, userId);
        return Result.success("订单创建成功", order);
    }

    @GetMapping("/list")
    public Result<List<StudentOrderVO>> getList(
        @RequestParam(required = false) String status,
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        List<StudentOrderVO> orders = orderService.getStudentOrderList(userId, status);
        return Result.success(orders);
    }

    @GetMapping("/{id}")
    public Result<StudentOrderVO> getDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        StudentOrderVO order = orderService.getStudentOrderDetail(id, userId);
        return Result.success(order);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.studentCancelOrder(id, userId);
        return Result.success("订单已取消", null);
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.studentConfirmReceipt(id, userId);
        return Result.success("订单已确认", null);
    }
}
