package com.campus.delivery.controller.student;

import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Complaint;
import com.campus.delivery.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/student/complaint")
public class StudentComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/list")
    public Result<List<Complaint>> getList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Complaint> list = complaintService.getListByUserId(userId);
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result<Complaint> add(@RequestBody Complaint complaint, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new com.campus.delivery.exception.BusinessException(401, "未授权，请先登录");
        }
        complaint.setUserId(userId);
        Complaint result = complaintService.add(complaint);
        return Result.success("投诉提交成功", result);
    }
}
