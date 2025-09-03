package com.sky.controller.admin;


import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
@Api(tags = "分类管理")
public class CategoryController {


    @Autowired
    private CategoryService categoryService;


    /**
     * 新增分类
     *
     * @param categoryDTO
     * @return
     */
    @ApiOperation("新增分类")
    @PostMapping
    public Result<String> save(@RequestBody CategoryDTO categoryDTO) {
        log.info("新增分类 {}", categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }


    /**
     * 分类分页查询
     *
     * @param categoryPageQueryDTO
     * @return
     */
    @ApiOperation("分类分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("分页查询: {}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }


    /**
     * 删除分类
     *
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除分类")
    public Result<String> deleteById(long id) {
        log.info("删除分类：{}", id);
        categoryService.deleteById(id);
        return Result.success();
    }


    /**
     * 修改分类
     *
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @ApiOperation("编辑分类")
    public Result<String> update(@RequestBody CategoryDTO categoryDTO) {
        log.info("编辑分类：{}", categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success();
    }


    /**
     * 启用禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用分类")
    public Result<String> startOrStop(@PathVariable(value = "status") Integer status, Long id) {
        log.info("启用禁用分类：{} , {}", status, id);
        categoryService.startOrStop(status, id);
        return Result.success();
    }

}