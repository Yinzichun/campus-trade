package com.campus.trade.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String city;
    private Integer gender;
    private String bankAccount;
    private String role;
    private String shopName;
    private String licenseImg;
    private String idCardImg;
    private String captchaKey;
    private String captchaCode;
}
