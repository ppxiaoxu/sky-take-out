package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api("店铺相关接口")
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setstatus(@PathVariable Integer status){
        // 将状态设置到Redis中
        redisTemplate.opsForValue().set(KEY , status);
        log.info("设置店铺营业状态：{}" ,  status == 1 ? "营业中" : "打烊中");
        return Result.success();
    }

    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        if (status == null) {
            log.info("获取店铺营业状态：未设置");
            // 可以根据业务需求返回默认值，比如0表示未设置
            return Result.success(0);
        }
        log.info("获取店铺营业状态：{}" , status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
