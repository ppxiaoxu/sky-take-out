package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;


public interface CategoryService {
    /**
     * 新增分类
     * @param category
     */
    void save(CategoryDTO category);

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 删除分类
     * @param id
     */
    void deleteById(long id);

    /**
     * 编辑（修改）分类
     * @param categoryDTO
     */
    void update(CategoryDTO categoryDTO);

    /**
     *  启用禁用分类
     * @param status
     * @param id
     */
    void startOrStop(Integer status, long id);

    /**
     * 查询分类
     *
     * @param type
     * @return
     */
    List<Category> list(Integer type);
}
