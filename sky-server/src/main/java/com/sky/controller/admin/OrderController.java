package com.sky.controller.admin;


import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;


    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch (OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单搜索：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 订单统计
     * @return
     */

    @GetMapping("/statistics")
    @ApiOperation("各个订单的数量统计")
    public Result<OrderStatisticsVO> statistics(){
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 管理端查看订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("管理端查看订单详情")
    public Result<OrderVO> detail(@PathVariable("id") Long id){
       OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }


    /**
     *商家接单
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("商家接单")
   public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("商家接单：{}", ordersConfirmDTO);
         orderService.confirm(ordersConfirmDTO);
        return Result.success();
   }

    /**
     * 商家拒单
     * @param ordersRejectionDTO
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("商家拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO){
        log.info("商家拒单：{}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("商家取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("商家取消订单:{}" , ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }


    /**
     * 商家派送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("商家派送订单")
     public Result delivery(@PathVariable("id") Long id){
        orderService.delivery(id);
        return Result.success();
     }





}
