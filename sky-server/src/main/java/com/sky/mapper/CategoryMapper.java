package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {

    /**
     *  新增分类
     * @param category
     */
    @Insert("insert into category(id, type, name, sort, status, create_time, update_time, create_user, update_user) " +
            "values " +
            "(#{id} , #{type} , #{name} , #{sort} , #{status} , #{createTime} , #{updateTime} , #{createUser} , #{updateUser})")
    void insert(Category category);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);
}
