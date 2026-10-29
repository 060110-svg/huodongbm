package com.stu.huodongbm.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ActivityScheduleTask {

    @Autowired
    private ActivityService activityService;

    @Scheduled(cron = "0 * * * * ?")
    public void updateActivityStatus() {
        Date now = new Date();

        LambdaUpdateWrapper<Activity> ongoingWrapper = new LambdaUpdateWrapper<>();
        ongoingWrapper.eq(Activity::getStatus, "published")
                .le(Activity::getStartTime, now)
                .gt(Activity::getEndTime, now)
                .set(Activity::getStatus, "ongoing");
        activityService.update(null, ongoingWrapper);

        LambdaUpdateWrapper<Activity> endedWrapper = new LambdaUpdateWrapper<>();
        endedWrapper.in(Activity::getStatus, "published", "ongoing")
                .le(Activity::getEndTime, now)
                .set(Activity::getStatus, "ended");
        activityService.update(null, endedWrapper);
    }
}