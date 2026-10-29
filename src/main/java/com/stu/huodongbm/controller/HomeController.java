package com.stu.huodongbm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.service.ActivityService;
import com.stu.huodongbm.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationService registrationService;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);

        long totalActivities = activityService.count();
        long ongoingActivities = activityService.count(new LambdaQueryWrapper<Activity>()
                .in(Activity::getStatus, "published", "ongoing"));
        long myTotal = registrationService.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, user.getId()));
        long myApproved = registrationService.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, user.getId())
                .eq(Registration::getStatus, "approved"));

        model.addAttribute("totalActivities", totalActivities);
        model.addAttribute("ongoingActivities", ongoingActivities);
        model.addAttribute("myTotal", myTotal);
        model.addAttribute("myApproved", myApproved);

        List<Activity> hotActivities = activityService.list(
                new LambdaQueryWrapper<Activity>()
                        .in(Activity::getStatus, "published", "ongoing")
                        .orderByDesc(Activity::getCurrentParticipants)
                        .orderByDesc(Activity::getCreateTime)
                        .last("LIMIT 5")
        );
        activityService.fillRegisteredCount(hotActivities);
        model.addAttribute("hotActivities", hotActivities);

        List<Registration> myLatest = registrationService.list(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getUserId, user.getId())
                        .orderByDesc(Registration::getRegisterTime)
                        .last("LIMIT 5")
        );
        for (Registration reg : myLatest) {
            Activity activity = activityService.getById(reg.getActivityId());
            if (activity != null) {
                reg.setActivityTitle(activity.getTitle());
            }
        }
        model.addAttribute("myLatest", myLatest);

        return "home";
    }
}