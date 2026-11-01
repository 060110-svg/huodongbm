package com.stu.huodongbm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.service.LoginAttemptService;
import com.stu.huodongbm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {//登录控制器
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserService userService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @GetMapping("/login")
    public String toLogin(){
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       HttpServletRequest request,
                       HttpSession session,
                       Model model){
        // 暴力破解防护（N7）：按 IP|用户名 限速
        String attemptKey = request.getRemoteAddr() + "|" + username;
        if (loginAttemptService.isBlocked(attemptKey)) {
            model.addAttribute("error", "失败次数过多，请10分钟后再试");
            return "login";
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userService.getOne(wrapper);

        if(user != null && passwordMatches(password, user.getPassword())){
            loginAttemptService.recordSuccess(attemptKey);
            // 存量明文密码在登录成功后自动升级为 BCrypt 哈希（惰性迁移）
            if (!isHashed(user.getPassword())) {
                user.setPassword(ENCODER.encode(password));
                userService.updateById(user);
            }
            // 会话中不保留密码
            user.setPassword(null);
            // 会话固定防护（N8）：登录成功后轮换会话
            session.invalidate();
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("user", user);
            if ("admin".equals(user.getRole())) {
                return "redirect:/index";
            } else {
                return "redirect:/home";
            }
        } else {
            loginAttemptService.recordFailure(attemptKey);
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }

    private static boolean isHashed(String stored) {
        // BCrypt 哈希恒为 60 位且以 $2 开头：避免恰好以 $2 开头的明文密码被误判为哈希而无法登录
        return stored != null && stored.length() == 60 && stored.startsWith("$2");
    }

    private static boolean passwordMatches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (isHashed(stored)) {
            return ENCODER.matches(raw, stored);
        }
        return stored.equals(raw);
    }
}