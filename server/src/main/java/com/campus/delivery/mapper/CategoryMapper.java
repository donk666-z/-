package com.campus.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.delivery.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}