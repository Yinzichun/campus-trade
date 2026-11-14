package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String name;
    private String phone;
    private String email;
    private String city;
    private Integer gender;
    private String bankAccount;
    private String role;
    private String status;
    private String shopName;
    private Integer merchantLevel;
    private String licenseImg;
    private String idCardImg;
    private BigDecimal balance;
    private Integer points;
    private BigDecimal creditScore;
    private LocalDateTime bannedUntil;
    private LocalDateTime createdAt;

    /** 手续费率，由等级换算，不入库 */
    @TableField(exist = false)
    private BigDecimal feeRate;
}
