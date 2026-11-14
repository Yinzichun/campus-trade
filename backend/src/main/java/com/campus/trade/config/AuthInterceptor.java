package com.campus.trade.config;

import com.campus.trade.common.Result;
import com.campus.trade.common.TokenUtil;
import com.campus.trade.common.UserContext;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器：解析请求头中的 token，把当前用户放入 ThreadLocal
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = TokenUtil.parse(token);
        if (userId == null) {
            write(response, 401, "请先登录");
            return false;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            write(response, 401, "用户不存在");
            return false;
        }
        if ("BANNED".equals(user.getStatus())) {
            write(response, 403, "账号已被封禁");
            return false;
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            write(response, 403, "账号尚未通过审核");
            return false;
        }
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void write(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, msg)));
        response.getWriter().flush();
    }
}
