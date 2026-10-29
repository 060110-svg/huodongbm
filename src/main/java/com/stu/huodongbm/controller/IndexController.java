package com.stu.huodongbm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.service.ActivityService;
import com.stu.huodongbm.service.RegistrationService;
import com.stu.huodongbm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @GetMapping("/index")
    public String index(HttpSession session, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        Object user = session.getAttribute("user");
        model.addAttribute("user", user);

        long activityCount = activityService.count();
        long publishedCount = activityService.count(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "published"));
        long registrationCount = registrationService.count(new LambdaQueryWrapper<Registration>().eq(Registration::getStatus, "registered"));
        long userCount = userService.count();

        model.addAttribute("activityCount", activityCount);
        model.addAttribute("publishedCount", publishedCount);
        model.addAttribute("registrationCount", registrationCount);
        model.addAttribute("userCount", userCount);

        List<Activity> recentActivities = activityService.list(
                new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getCreateTime).orderByDesc(Activity::getId).last("LIMIT 5")
        );
        activityService.fillRegisteredCount(recentActivities);
        model.addAttribute("recentActivities", recentActivities);

        return "index";
    }
}