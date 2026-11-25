package com.stu.huodongbm.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stu.huodongbm.common.LoginUser;
import com.stu.huodongbm.common.RequireAdmin;
import com.stu.huodongbm.common.Result;
import com.stu.huodongbm.common.UserContext;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.service.ActivityService;
import com.stu.huodongbm.service.RegistrationService;
import com.stu.huodongbm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiIndexController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @RequireAdmin
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return Result.fail(401, "未登录");
        }

        long activityCount = activityService.count();
        long publishedCount = activityService.count(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published"));
        long registrationCount = registrationService.count(new LambdaQueryWrapper<Registration>().eq(Registration::getStatus, "registered"));
        long userCount = userService.count();

        List<Activity> recentActivities = activityService.list(
                new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getCreateTime).orderByDesc(Activity::getId).last("LIMIT 5")
        );
        activityService.fillRegisteredCount(recentActivities);

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("activityCount", activityCount);
        data.put("publishedCount", publishedCount);
        data.put("registrationCount", registrationCount);
        data.put("userCount", userCount);
        data.put("recentActivities", recentActivities);
        return Result.ok(data);
    }
}