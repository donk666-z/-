package com.campus.delivery.controller.merchant;

import com.campus.delivery.common.Result;
import com.campus.delivery.dto.StudentOrderVO;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.service.MerchantService;
import com.campus.delivery.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/merchant/order")
public class MerchantOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/list")
    public Result<List<StudentOrderVO>> getList(
        @RequestParam(required = false) String status,
        HttpServletRequest request
    ) {
        Merchant merchant = requireMerchant(request);
        List<StudentOrderVO> orders = orderService.getMerchantOrderList(merchant.getId(), status);
        return Result.success(orders);
    }

    @GetMapping("/{id}")
    public Result<StudentOrderVO> getDetail(@PathVariable Long id, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        StudentOrderVO order = orderService.getMerchantOrderDetail(id, merchant.getId());
        return Result.success(order);
    }

    @GetMapping("/pending")
    public Result<List<StudentOrderVO>> getPending(HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        List<StudentOrderVO> orders = orderService.getMerchantOrderList(merchant.getId(), "paid");
        return Result.success(orders);
    }

    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        orderService.getMerchantOrderDetail(id, merchant.getId());
        orderService.acceptOrder(id);
        return Result.success("订单已接单", null);
    }

    @PostMapping("/{id}/prepare")
    public Result<Void> prepare(@PathVariable Long id, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        orderService.getMerchantOrderDetail(id, merchant.getId());
        orderService.prepareOrder(id);
        return Result.success("餐品已备好", null);
    }

    private Merchant requireMerchant(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getByUserId(userId);
        if (merchant == null) {
            throw new BusinessException(404, "商户不存在");
        }
        return merchant;
    }
}
