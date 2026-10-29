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
            String uri = request.getRequestURI();
            User u = (User) user;

            if (uri.startsWith("/activity/toAdd") || uri.startsWith("/activity/toEdit")
                    || uri.startsWith("/activity/save") || uri.startsWith("/activity/update")
                    || uri.startsWith("/activity/delete")
                    || uri.startsWith("/registration/list")) {
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