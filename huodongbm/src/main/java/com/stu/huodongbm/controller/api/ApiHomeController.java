package com.stu.huodongbm.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stu.huodongbm.common.LoginUser;
import com.stu.huodongbm.common.Result;
import com.stu.huodongbm.common.UserContext;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.service.ActivityService;
import com.stu.huodongbm.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiHomeController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationService registrationService;

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return Result.fail(401, "未登录");
        }

        long totalActivities = activityService.count();
        long ongoingActivities = activityService.count(new LambdaQueryWrapper<Activity>()
                .in(Activity::getStatus, "published", "ongoing"));
        long myTotal = registrationService.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, user.getId()));
        long myApproved = registrationService.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, user.getId())
                .eq(Registration::getStatus, "approved"));

        List<Activity> hotActivities = activityService.list(
                new LambdaQueryWrapper<Activity>()
                        .in(Activity::getStatus, "published", "ongoing")
                        .orderByDesc(Activity::getCurrentParticipants)
                        .orderByDesc(Activity::getCreateTime)
                        .last("LIMIT 5")
        );
        activityService.fillRegisteredCount(hotActivities);

        List<Registration> myLatest = registrationService.list(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getUserId, user.getId())
                        .orderByDesc(Registration::getRegisterTime)
                        .last("LIMIT 5")
        );
        if (!myLatest.isEmpty()) {
            List<Integer> activityIds = myLatest.stream()
                    .map(Registration::getActivityId).distinct().toList();
            Map<Integer, String> titleMap = new HashMap<>();
            for (Activity activity : activityService.listByIds(activityIds)) {
                titleMap.put(activity.getId(), activity.getTitle());
            }
            for (Registration reg : myLatest) {
                reg.setActivityTitle(titleMap.get(reg.getActivityId()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("totalActivities", totalActivities);
        data.put("ongoingActivities", ongoingActivities);
        data.put("myTotal", myTotal);
        data.put("myApproved", myApproved);
        data.put("hotActivities", hotActivities);
        data.put("myLatest", myLatest);
        return Result.ok(data);
    }
}