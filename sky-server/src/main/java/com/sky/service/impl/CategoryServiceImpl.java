package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


import java.time.LocalDateTime;
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();

        //对象属性拷贝
        BeanUtils.copyProperties(categoryDTO,category);

        //设置分类状态，默认为0
        category.setStatus(StatusConstant.DISABLE);

        //设置创建时间与修改时间
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());

        //设置当前创建人id与修改人id
        category.setCreateUser(BaseContext.getCurrentId());
        category.setUpdateUser(BaseContext.getCurrentId());

        //插入菜品数据
        categoryMapper.insert(category);
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        //开启分页查询
        PageHelper.startPage(categoryPageQueryDTO.getPage() , categoryPageQueryDTO.getPageSize());

        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        return new PageResult(page.getTotal() , page.getResult());
    }

    /**
     * 删除分类
     * @param id
     */
    public void deleteById(long id) {
        //判断当前分类是否关联了菜品或者套餐，如果关联了，则无法删除，抛出业务异常
        Integer count = dishMapper.countByCategoryId(id);
        if(count > 0){
            /**
             * 当前分类关联了菜品，则无法删除，抛出信息错误异常
             */
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        count = setmealMapper.countByCategoryId(id);
        if(count > 0){
            /**
             * 当前分类关联了套餐，则无法删除，抛出信息错误异常
             */
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //删除分类数据
        categoryMapper.deleteById(id);

    }

   /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        //对象属性拷贝
        BeanUtils.copyProperties(categoryDTO,category);
        //设置修改时间
        category.setCreateTime(LocalDateTime.now());
        //设置当前修改人
        category.setCreateUser(BaseContext.getCurrentId());

        //修改分类
        categoryMapper.update(category);



    }

    /**
     * 启用禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, long id) {
        Category category = new Category();
        category.setStatus(status);
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        category.setUpdateUser(BaseContext.getCurrentId());
        categoryMapper.update(category);
    }
}
