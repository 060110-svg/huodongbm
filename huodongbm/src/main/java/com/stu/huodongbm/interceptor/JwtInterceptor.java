package com.stu.huodongbm.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stu.huodongbm.common.LoginUser;
import com.stu.huodongbm.common.RequireAdmin;
import com.stu.huodongbm.common.Result;
import com.stu.huodongbm.common.UserContext;
import com.stu.huodongbm.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (token == null || !jwtUtil.validate(token)) {
            writeJson(response, 401, Result.fail(401, "未登录或登录已过期，请重新登录"));
            return false;
        }

        try {
            Claims claims = jwtUtil.parse(token);
            Integer userId = claims.get("userId", Integer.class);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            String realName = claims.get("realName", String.class);
            LoginUser loginUser = new LoginUser(userId, username, realName, role);
            UserContext.set(loginUser);

            if (requireAdmin(handler) && !loginUser.isAdmin()) {
                // preHandle 返回 false 时不会回调 afterCompletion，必须在此清理，
                // 否则 ThreadLocal 残留在线程池线程上，后续复用该线程的请求会读到旧身份
                UserContext.clear();
                writeJson(response, 403, Result.fail(403, "无权限访问该资源"));
                return false;
            }
            return true;
        } catch (Exception e) {
            writeJson(response, 401, Result.fail(401, "登录信息无效，请重新登录"));
            return false;
        }
    }

    private boolean requireAdmin(Object handler) {
        if (handler instanceof HandlerMethod hm) {
            return hm.hasMethodAnnotation(RequireAdmin.class)
                    || hm.getBeanType().isAnnotationPresent(RequireAdmin.class);
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeJson(HttpServletResponse response, int status, Result<?> result) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}