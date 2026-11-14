package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reviews")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long merchantId;
    private Long buyerId;
    private String type;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String buyerName;
    @TableField(exist = false)
    private String productName;
}
