package com.campus.delivery.controller.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Order;
import com.campus.delivery.entity.Review;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.mapper.OrderMapper;
import com.campus.delivery.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/student/review")
public class StudentReviewController {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @PostMapping
    public Result<Void> create(@RequestBody Review review, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (review.getOrderId() == null) {
            throw new BusinessException(400, "缺少订单信息");
        }

        LambdaQueryWrapper<Review> dup = new LambdaQueryWrapper<>();
        dup.eq(Review::getOrderId, review.getOrderId());
        if (reviewMapper.selectCount(dup) > 0) {
            throw new BusinessException(400, "该订单已评价");
        }

        Order order = orderMapper.selectById(review.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权评价该订单");
        }
        if (!"completed".equals(order.getStatus())) {
            throw new BusinessException(400, "仅限已完成订单评价");
        }

        review.setUserId(userId);
        review.setMerchantId(order.getMerchantId());
        reviewMapper.insert(review);
        return Result.success("评价成功", null);
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<Review>> getByMerchant(@PathVariable Long merchantId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getMerchantId, merchantId)
               .orderByDesc(Review::getCreatedAt);
        List<Review> reviews = reviewMapper.selectList(wrapper);
        return Result.success(reviews);
    }
}
