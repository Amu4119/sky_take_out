package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品
     * */
    void saveWithFlavor(DishDTO dishDTO);

    /**
     * 菜品条件分页查询
     * */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品批量删除
     */
    void deleteBatch(List<Long> ids);
}
