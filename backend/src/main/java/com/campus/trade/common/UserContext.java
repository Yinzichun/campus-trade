package com.campus.trade.common;

import com.campus.trade.entity.User;

/**
 * 当前登录用户上下文，由拦截器写入，请求结束时清理
 */
public class UserContext {

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    public static Long userId() {
        User u = HOLDER.get();
        if (u == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return u.getId();
    }

    public static void requireRole(String role) {
        User u = HOLDER.get();
        if (u == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        if (!role.equals(u.getRole())) {
            throw new BizException(403, "没有操作权限");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
