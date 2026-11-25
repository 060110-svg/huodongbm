package com.stu.huodongbm.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stu.huodongbm.common.LoginRequest;
import com.stu.huodongbm.common.LoginUser;
import com.stu.huodongbm.common.Result;
import com.stu.huodongbm.common.UserContext;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.service.LoginAttemptService;
import com.stu.huodongbm.service.UserService;
import com.stu.huodongbm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiLoginController {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserService userService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        String username = req.getUsername();
        String password = req.getPassword();
        if (username == null || username.trim().isEmpty() || password == null) {
            return Result.fail("用户名和密码不能为空");
        }

        String attemptKey = request.getRemoteAddr() + "|" + username;
        if (loginAttemptService.isBlocked(attemptKey)) {
            return Result.fail("失败次数过多，请10分钟后再试");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userService.getOne(wrapper);

        if (user != null && passwordMatches(password, user.getPassword())) {
            loginAttemptService.recordSuccess(attemptKey);
            if (!isHashed(user.getPassword())) {
                user.setPassword(ENCODER.encode(password));
                userService.updateById(user);
            }

            String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRealName(), user.getRole());

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
            data.put("user", loginUser);
            return Result.ok(data);
        } else {
            loginAttemptService.recordFailure(attemptKey);
            return Result.fail("用户名或密码错误");
        }
    }

    @GetMapping("/user/info")
    public Result<LoginUser> info() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return Result.fail(401, "未登录");
        }
        return Result.ok(user);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    private static boolean isHashed(String stored) {
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