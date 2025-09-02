package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.result.PageResult;
import org.apache.ibatis.annotations.Mapper;


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
}
