package com.campus.delivery.controller.rider;

import com.campus.delivery.common.Result;
import com.campus.delivery.dto.RiderProfileVO;
import com.campus.delivery.service.RiderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/rider/profile")
public class RiderProfileController {

    @Autowired
    private RiderService riderService;

    @GetMapping
    public Result<RiderProfileVO> getProfile(HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        return Result.success(riderService.getProfile(riderId));
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long riderId = (Long) request.getAttribute("userId");
        String status = params.get("status");
        if (!StringUtils.hasText(status)) {
            return Result.error("骑手状态不能为空");
        }
        riderService.updateStatus(riderId, status);
        return Result.success("状态更新成功", null);
    }
}
