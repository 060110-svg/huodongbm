package com.stu.huodongbm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * CSRF 防护（自研轻量同步令牌方案，项目未引入 Spring Security）。
 *
 * 机制：
 * - 每个请求确保会话中存在随机令牌（session 属性 csrfToken）；
 * - 安全方法（GET/HEAD 等）直接放行；
 * - POST 请求必须携带与会话一致的 _csrf 参数，否则返回 403。
 *
 * 模板中的 POST 表单统一注入：
 * <input type="hidden" name="_csrf" th:value="${session.csrfToken}">
 *
 * 说明：使用 MessageDigest.isEqual 做常数时间比对，避免时序侧信道。
 */
@Component
public class CsrfFilter extends OncePerRequestFilter {

    public static final String TOKEN_SESSION_ATTR = "csrfToken";
    public static final String TOKEN_PARAM = "_csrf";

    /**
     * 静态资源请求不参与 CSRF 处理，避免为爬虫/静态资源无条件创建会话导致 session 膨胀。
     * 排除范围与 WebConfig 中登录拦截器的静态资源排除项保持一致；
     * HTML 页面路径不排除（模板渲染表单需要 session 中的 token）。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/uploads/")
                || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(TOKEN_SESSION_ATTR);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(TOKEN_SESSION_ATTR, token);
        }

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String submitted = request.getParameter(TOKEN_PARAM);
            if (submitted == null || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8),
                    submitted.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "表单已过期或来源非法，请刷新页面后重试");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
