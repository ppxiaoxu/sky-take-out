package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end的范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while(!begin.equals(end)){
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();

        for(LocalDate date : dateList){
            //查询date日期对应的营业额,营业额是指：状态为“已完成”的订单金额统计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select sum(amount) from orders where order_time > beginTime and order_time < endTime and status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            //如果turnover为null，则说明没有对应的数据，则赋值为0.0
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();

    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
         //当前集合用于存放从begin到end的范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的新增用户数
        List<Integer> newuserList = new ArrayList<>();
        //存放每天的总用户数
        List<Integer> totalUserList = new ArrayList<>();

        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end" , endTime);

            //总用户数量
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);

            //新增用户数量
            Integer newUsers = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newuserList.add(newUsers);
        }
        //封装结果数据
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newuserList, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end的范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的有效订单数 select count(id) from orders where order_time > beginTime and order_time < endTime
        List<Integer> validOrderCountList = new ArrayList<>();
        //存放每天的订单总数 select count(id) from orders where order_time > beginTime and order_time < endTime and status = 5
        List<Integer> orderCountList = new ArrayList<>();

        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //订单总数
            Integer totalOrders = getOrderCount(beginTime, endTime, null);
            //有效订单数
            Integer validOrders = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(totalOrders);
            validOrderCountList.add(validOrders);

        }

        //计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
        Integer vaildOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //计算订单完成率
        Double orderCompletionRate = 0.0;

        if (totalOrderCount != 0) {
            orderCompletionRate = vaildOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(vaildOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    //统计指定时间区间内订单数量
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap();
        map.put("begin" , begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);

    }


    /**
     * 查询销量排行top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        //查询top10
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        //将List<GoodsSalesDTO>转化为List<String>
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        //封装结果数据
        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();

    }
}
