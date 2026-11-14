package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BizException;
import com.campus.trade.common.TokenUtil;
import com.campus.trade.dto.RegisterDTO;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final String SALT = "campus-trade";

    private final UserMapper userMapper;
    private final CaptchaService captchaService;

    public AuthService(UserMapper userMapper, CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.captchaService = captchaService;
    }

    public Map<String, Object> register(RegisterDTO dto) {
        if (!captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BizException("验证码错误或已过期");
        }
        if (dto.getUsername() == null || dto.getUsername().trim().length() < 3) {
            throw new BizException("账号至少 3 位");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BizException("请填写姓名");
        }
        if (dto.getPhone() != null && !dto.getPhone().matches("1\\d{10}")) {
            throw new BizException("手机号格式不正确");
        }
        if (dto.getBankAccount() != null && !dto.getBankAccount().isEmpty()
                && !dto.getBankAccount().matches("\\d{16}")) {
            throw new BizException("银行账号必须是 16 位数字");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername().trim()));
        if (count > 0) {
            throw new BizException("该账号已被注册");
        }

        String role = "MERCHANT".equalsIgnoreCase(dto.getRole()) ? "MERCHANT" : "USER";
        if ("MERCHANT".equals(role)) {
            if (isBlank(dto.getLicenseImg()) || isBlank(dto.getIdCardImg())) {
                throw new BizException("商家注册必须上传营业执照和身份证照片");
            }
            if (isBlank(dto.getShopName())) {
                throw new BizException("请填写店铺名称");
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(hash(dto.getPassword()));
        user.setName(dto.getName().trim());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setCity(dto.getCity());
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setBankAccount(dto.getBankAccount());
        user.setRole(role);
        user.setStatus("PENDING");
        user.setBalance(BigDecimal.ZERO);
        user.setPoints(0);
        user.setCreditScore(new BigDecimal("100.00"));
        user.setCreatedAt(LocalDateTime.now());
        if ("MERCHANT".equals(role)) {
            user.setShopName(dto.getShopName());
            user.setMerchantLevel(1);
            user.setLicenseImg(dto.getLicenseImg());
            user.setIdCardImg(dto.getIdCardImg());
        }
        userMapper.insert(user);
        return Map.of("userId", user.getId(), "status", "PENDING");
    }

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !hash(password).equals(user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if ("REJECTED".equals(user.getStatus())) {
            throw new BizException("注册申请已被驳回，请联系管理员");
        }
        if ("PENDING".equals(user.getStatus())) {
            throw new BizException("账号正在审核中，请等待管理员审核通过");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BizException("账号已被封禁至 " + user.getBannedUntil());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", TokenUtil.create(user.getId()));
        data.put("user", user);
        return data;
    }

    public User current(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((raw + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
