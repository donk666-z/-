package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.entity.Complaint;
import com.campus.delivery.mapper.ComplaintMapper;
import com.campus.delivery.service.ComplaintService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintServiceImpl extends ServiceImpl<ComplaintMapper, Complaint> implements ComplaintService {

    @Override
    public List<Complaint> getListByUserId(Long userId) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Complaint::getUserId, userId);
        wrapper.orderByDesc(Complaint::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public Complaint add(Complaint complaint) {
        complaint.setStatus("pending");
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        save(complaint);
        return complaint;
    }

    @Override
    public Complaint reply(Long id, String reply) {
        Complaint complaint = getById(id);
        if (complaint != null) {
            complaint.setReply(reply);
            complaint.setStatus("resolved");
            complaint.setUpdatedAt(LocalDateTime.now());
            updateById(complaint);
        }
        return complaint;
    }
}
