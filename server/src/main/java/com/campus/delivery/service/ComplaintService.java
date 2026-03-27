package com.campus.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.delivery.entity.Complaint;
import java.util.List;

public interface ComplaintService extends IService<Complaint> {

    List<Complaint> getListByUserId(Long userId);

    Complaint add(Complaint complaint);

    Complaint reply(Long id, String reply);
}
