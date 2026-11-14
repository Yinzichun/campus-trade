package com.campus.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.campus.trade.mapper")
@SpringBootApplication
public class CampusTradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusTradeApplication.class, args);
        System.out.println("======== 校园二手交易平台启动成功 http://localhost:8081 ========");
    }
}
