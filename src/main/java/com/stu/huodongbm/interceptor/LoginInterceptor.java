package com.stu.huodongbm.interceptor;

import com.stu.huodongbm.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");

        if (user != null) {
            // 应用内路径 = requestURI 去掉 contextPath：容器无关（Tomcat/MockMvc 行为一致），
            // 且未来配置 context-path 后前缀匹配依然有效
            String uri = request.getRequestURI().substring(request.getContextPath().length());
            User u = (User) user;

            if (uri.startsWith("/activity/toAdd") || uri.startsWith("/activity/toEdit")
                    || uri.startsWith("/activity/save") || uri.startsWith("/activity/update")
                    || uri.startsWith("/activity/delete")
                    || uri.startsWith("/registration/list")
                    || uri.startsWith("/registration/approve")
                    || uri.startsWith("/registration/reject")
                    || uri.startsWith("/registration/reaudit")
                    || uri.startsWith("/index")) {
                if (!"admin".equals(u.getRole())) {
                    response.sendRedirect("/activity/list");
                    return false;
                }
            }
            return true;
        }

        response.sendRedirect("/login");
        return false;
    }
}