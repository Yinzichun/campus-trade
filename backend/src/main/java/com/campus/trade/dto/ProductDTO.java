package com.campus.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private Integer categoryId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String size;
    private String description;
    private List<String> images;
    private Integer negotiable;
    private Integer stock;
    private String conditionLevel;
}
