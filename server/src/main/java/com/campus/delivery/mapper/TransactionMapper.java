package com.campus.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.delivery.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {
}
