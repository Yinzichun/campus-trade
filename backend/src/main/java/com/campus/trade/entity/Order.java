package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long merchantId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal originAmount;
    private BigDecimal fee;
    private Integer pointsUsed;
    private BigDecimal pointsDeduct;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime payTime;
    private LocalDateTime confirmDeadline;
    private LocalDateTime receiveTime;
    private LocalDateTime returnDeadline;
    private String returnReason;
    private LocalDateTime settleTime;

    @TableField(exist = false)
    private String buyerName;
    @TableField(exist = false)
    private String shopName;
    @TableField(exist = false)
    private Boolean reviewed;
}
