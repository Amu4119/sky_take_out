package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 * */
@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品管理")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * */
    @PostMapping
    @ApiOperation("新增菜品")
    @CacheEvict(cacheNames = "dish_", key = "#dishDTO.categoryId")
    public Result save(@RequestBody DishDTO dishDTO){
        dishService.saveWithFlavor(dishDTO);

        //新增菜品，清除redis中以dish_categoryId开头的数据
//        String key = "dish_" + dishDTO.getCategoryId();
//        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品条件分页查询
     * */
    @GetMapping("/page")
    @ApiOperation("菜品条件分页查询")
    public Result page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品条件分页查询:{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品批量删除
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    @CacheEvict(cacheNames = "dish_", allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除:{}",ids);
        dishService.deleteBatch(ids);

        //菜品批量删除,清除redis中所有以dish_开头的数据
//        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result getByIdWithFlavor(@PathVariable Long id){
        log.info("根据id查询菜品");
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    @CacheEvict(cacheNames = "dishCache", allEntries = true)
    public Result updateWithFlavor(@RequestBody DishDTO dishDTO){
        log.info("修改菜品:{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //修改菜品，清除redis中所有以dish_开头的数据
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 启用、禁用菜品
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用菜品")
    @CacheEvict(cacheNames = "dishCache", allEntries = true)
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id) {
        dishService.startOrStop(status, id);

        //修改菜品，清除redis中所有以dish_开头的数据
        cleanCacheWithDish("dish_*");

        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    //创建一个统一的，用于清除redis缓存的方法
    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

    //创建一个统一的，用于清除redis中所有以dish_开头的缓存的方法
    private void cleanCacheWithDish(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
