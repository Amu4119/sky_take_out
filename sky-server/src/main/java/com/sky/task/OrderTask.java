package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理支付超时的订单
     */
    @Scheduled(cron = "0 * * * * *") // 每分钟处理一次
//    @Scheduled(cron = "0/5 * * * * *") // 从第0秒开始，每5秒处理一次
    public void processTimeoutOrder(){
        log.info("处理支付超时的订单:{}",LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        //查询支付超时的订单
        List<Orders> ordersList =  orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT,time);

        //更新超时订单的状态
        if (ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * *") //每天凌晨1点处理一次
//    @Scheduled(cron = "1/5 * * * * *") // 从第1秒开始，每5秒处理一次
    public void processDeliveryOrder(){
        log.info("处理一直处于派送中的订单:{}",LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        //处理一直处于派送中的、前一天的订单
        List<Orders> ordersList =  orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS,time);

        //更新超时订单的状态
        if (ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
