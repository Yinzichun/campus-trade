package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("products")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Integer categoryId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String size;
    private String description;
    private String images;
    private Integer negotiable;
    private Integer stock;
    private String conditionLevel;
    private String status;
    private Integer sales;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 非数据库字段：店铺名、商家名、分类名、图片数组 */
    @TableField(exist = false)
    private String shopName;
    @TableField(exist = false)
    private String merchantName;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private List<String> imageList;
}
