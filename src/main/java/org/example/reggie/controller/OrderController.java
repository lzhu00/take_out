package org.example.reggie.controller;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.R;
import org.example.reggie.dto.OrdersDto;
import org.example.reggie.entity.OrderDetail;
import org.example.reggie.entity.Orders;
import org.example.reggie.service.OrderDetailService;
import org.example.reggie.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        orderService.submit(orders);
        return R.success("提交订单");
    }

    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize){
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> dtoPage = new Page<>(page, pageSize);

        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, userId);
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo, queryWrapper);

        List<OrdersDto> list = pageInfo.getRecords().stream().map((item) -> {
            Long orderId = item.getId();
            OrdersDto ordersDto = new OrdersDto();
            LambdaQueryWrapper<OrderDetail> queryWrapperDetail = new LambdaQueryWrapper<>();
            queryWrapperDetail.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetails = orderDetailService.list(queryWrapperDetail);

            ordersDto.setOrderDetails(orderDetails);

            BeanUtils.copyProperties(item, ordersDto);

            return ordersDto;
        }).collect(Collectors.toList());

        BeanUtils.copyProperties(pageInfo, dtoPage);
        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime, String endTime){
        Page<Orders> pageOrder = new Page<>(page, pageSize);
        Page<OrdersDto> pageDto = new Page<>(page, pageSize);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(number != null, Orders::getId, number);
        queryWrapper.ge(!StringUtils.isEmpty(beginTime), Orders::getOrderTime, beginTime).le(!StringUtils.isEmpty(endTime), Orders::getOrderTime, endTime);
        queryWrapper.orderByDesc(Orders::getOrderTime);

        orderService.page(pageOrder, queryWrapper);

        List<OrdersDto> dtos = pageOrder.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            LambdaQueryWrapper<OrderDetail> wrapperDetail = new LambdaQueryWrapper<>();
            wrapperDetail.eq(OrderDetail::getOrderId, item.getId());

            List<OrderDetail> details = orderDetailService.list(wrapperDetail);

            ordersDto.setOrderDetails(details);
            BeanUtils.copyProperties(item, ordersDto);
            return ordersDto;
        }).collect(Collectors.toList());

        pageDto.setRecords(dtos);
        BeanUtils.copyProperties(pageOrder, pageDto);

        return R.success(pageDto);
    }

    @PutMapping
    public R<String> consign(@RequestBody Orders order){
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, order.getId());
        wrapper.set(Orders::getStatus, order.getStatus());
        orderService.update(wrapper);

        return R.success("修改订单状态");
    }
}
