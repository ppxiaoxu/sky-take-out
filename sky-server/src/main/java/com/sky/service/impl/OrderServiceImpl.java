package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PutMapping;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    private Orders orders;


    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        //处理业务异常（地址簿为空，购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //地址簿为空,不能下单
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查询用户当前的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        //判断购物车数据是否为空
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表中插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);//待支付
        orders.setStatus(Orders.PENDING_PAYMENT);//待付款
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());//收货人
        orders.setAddress(addressBook.getDetail());
        orders.setUserId(userId);

        this.orders = orders;

        orderMapper.insert(orders);


        List<OrderDetail> orderDetailList = new ArrayList<>();
        //向订单明细表插入n条数据
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);

        }
        orderDetailMapper.insert(orderDetailList);
        //下单成功，清空购物车数据
        shoppingCartMapper.deleteShoppingCart(shoppingCart);

        //封装一个VO对象返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orders.getId());
        return vo;

    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }


    /**
     * 用户取消订单
     *
     * @param id
     */
    public void cancel(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 模拟退款操作 - 注释掉原来的微信退款调用
            log.info("模拟退款操作，订单号: {}, 退款金额: 0.01元", ordersDB.getNumber());

            // 原来的微信退款调用（注释掉）
            // weChatPayUtil.refund(
            //         ordersDB.getNumber(), //商户订单号
            //         ordersDB.getNumber(), //商户退款单号
            //         new BigDecimal(0.01),//退款金额，单位 元
            //         new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);


    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    public void OneMoreOrder(Long id) {
        //当前登录用户id
        Long userId = BaseContext.getCurrentId();
        //根据订单id查询订单数据
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);


//            循环插入购物车数据
//            for(OrderDetail orderDetail : orderDetailList){
//                ShoppingCart shoppingCart = new ShoppingCart();
//                shoppingCart.setUserId(userId);
//                shoppingCart.setDishId(orderDetail.getDishId());
//                shoppingCart.setSetmealId(orderDetail.getSetmealId());
//                shoppingCart.setName(orderDetail.getName());
//                shoppingCart.setDishFlavor(orderDetail.getDishFlavor());
//                shoppingCart.setImage(orderDetail.getImage());
//                shoppingCart.setAmount(orderDetail.getAmount());
//                shoppingCart.setNumber(orderDetail.getNumber());
//                shoppingCart.setCreateTime(LocalDateTime.now());
//                shoppingCartMapper.insert(shoppingCart);
//            }

        //批量插入数据
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            //将原订单的数据复制到购物车
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        //将购物车对象批量插入数据库
        shoppingCartMapper.insertBatch(shoppingCartList);


    }

    /**
     * 条件搜索订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //部分订单状态需要额外返回菜品信息，将Orders 对象转为 OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(), orderVOList);
    }



    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOlist = new ArrayList<>();
        List<Orders> ordersList = page.getResult();

        //CollectionUtils 是 Spring 框架提供的一个工具类，
        //专门用来处理集合（List、Set等）的常见操作，特别是空值安全检查。
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                //将菜品信息拼接为一个字符串，格式为：菜品*数量;菜品*数量
                String orderDishs = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishs);
                orderVOlist.add(orderVO);

            }

        }
        return orderVOlist;

    }

    private String getOrderDishesStr(Orders orders) {
        //根据id查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> dishList = orderDetailList.stream().map(orderDetail -> {
            String dish = orderDetail.getName() + "*" + orderDetail.getNumber();
            return dish;
        }).collect(Collectors.toList());

        return String.join(";", dishList);

    }


    /**
     * 各个订单数量统计
     */
    public OrderStatisticsVO statistics() {
        //根据订单状态分别查询订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        //封装结果数据
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;


    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     * @return
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);



    }

    /**
     * 商家拒单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //根据id查询订单数据
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        if(ordersDB == null || ordersDB.getStatus() != Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);

        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        Integer payStatus = ordersDB.getPayStatus();
        // 订单处于待接单状态下取消，需要进行退款
        if (payStatus == Orders.PAID) {
            // 模拟退款操作 - 注释掉原来的微信退款调用
            log.info("模拟退款操作，订单号: {}, 退款金额: 0.01元", ordersDB.getNumber());

            // 原来的微信退款调用（注释掉）
            // weChatPayUtil.refund(
            //         ordersDB.getNumber(), //商户订单号
            //         ordersDB.getNumber(), //商户退款单号
            //         new BigDecimal(0.01),//退款金额，单位 元
            //         new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);


    }

    /**
     * 商家取消订单
     * * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //根据id查询订单信息
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        Integer payStatus = ordersDB.getPayStatus();
        if(payStatus == Orders.PAID){
            //完成支付，需要退款
            log.info("模拟微信支付退款操作");
            // 支付状态修改为 退款
            ordersDB.setPayStatus(Orders.REFUND);
        }
        //更新订单状态，设置订单取消原因
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }


}















