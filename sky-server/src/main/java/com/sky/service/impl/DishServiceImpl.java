package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 新增菜品
     * */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        //对象属性拷贝
        BeanUtils.copyProperties(dishDTO,dish);

        //为菜品表插入一条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //为菜品口味表插入多条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品条件分页查询
     * */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        //开始分页查询
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        //封装查询数据
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 菜品批量删除
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //起售中菜品不能删除
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //被套餐关联的菜品不能删除
        List<Long> setMealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setMealIds != null && !setMealIds.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //批量删除菜品
        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品对应的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品信息
        Dish dish = dishMapper.getById(id);
        //查询菜品对应的口味信息
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //封装数据
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //更新菜品基础信息
        dishMapper.update(dish);
        //删除菜品对应的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //插入菜品新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //为菜品口味表插入多条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        //在查询该分类下的菜品之前，先查询该分类是否是起售状态，如果不是起售状态，则返回空
        /*int status = categoryMapper.getStatusByCategoryId(dish.getCategoryId());
        if (status == 0){
            return null;
        }*/

        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 启用、禁用菜品
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //更新菜品状态
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        //更新包含此菜品的套餐状态
        List<Long> setmealIds = setmealDishMapper.getByDishId(id);
        if (setmealIds != null && !setmealIds.isEmpty()){
            for (Long setmealId : setmealIds) {
                Setmeal setmeal = Setmeal.builder()
                        .id(setmealId)
                        .status(status)
                        .build();
                setmealMapper.update(setmeal);
            }
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }}
