package com.campus.trade.task;

import com.campus.trade.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每 60 秒扫描一次，把超过 7 天未确认收货的订单自动结算
 */
@Slf4j
@Component
public class OrderSettleTask {

    private final OrderService orderService;

    public OrderSettleTask(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        int count = orderService.autoConfirm();
        if (count > 0) {
            log.info("自动确认收货 {} 笔订单", count);
        }
    }
}
