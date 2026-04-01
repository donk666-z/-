package com.campus.delivery.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Complaint;
import com.campus.delivery.entity.User;
import com.campus.delivery.mapper.UserMapper;
import com.campus.delivery.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/complaint")
public class AdminComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> getList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Complaint::getStatus, status.trim());
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Complaint::getType, type.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Complaint::getContent, kw).or().like(Complaint::getContact, kw));
        }
        wrapper.orderByAsc(Complaint::getStatus)
                .orderByDesc(Complaint::getCreatedAt);

        Page<Complaint> sourcePage = complaintService.page(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        targetPage.setRecords(toRecords(sourcePage.getRecords()));
        return Result.success(targetPage);
    }

    @PutMapping("/{id}/reply")
    public Result<Complaint> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Complaint complaint = complaintService.getById(id);
        if (complaint == null) {
            return Result.error(404, "投诉记录不存在");
        }
        String reply = body == null ? null : body.get("reply");
        String status = body == null ? null : body.get("status");
        if (!StringUtils.hasText(reply)) {
            return Result.error(400, "回复内容不能为空");
        }
        complaint.setReply(reply.trim());
        complaint.setStatus(StringUtils.hasText(status) ? status.trim() : "resolved");
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintService.updateById(complaint);
        return Result.success("处理成功", complaint);
    }

    private List<Map<String, Object>> toRecords(List<Complaint> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = records.stream()
                .map(Complaint::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Complaint complaint : records) {
            User user = userMap.get(complaint.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", complaint.getId());
            item.put("userId", complaint.getUserId());
            item.put("userName", user == null ? "" : user.getNickname());
            item.put("userPhone", user == null ? "" : user.getPhone());
            item.put("type", complaint.getType());
            item.put("content", complaint.getContent());
            item.put("contact", complaint.getContact());
            item.put("status", complaint.getStatus());
            item.put("reply", complaint.getReply());
            item.put("createdAt", complaint.getCreatedAt());
            item.put("updatedAt", complaint.getUpdatedAt());
            list.add(item);
        }
        return list;
    }
}
