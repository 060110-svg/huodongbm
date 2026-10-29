package com.stu.huodongbm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.mapper.ActivityMapper;
import com.stu.huodongbm.mapper.RegistrationMapper;
import com.stu.huodongbm.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Override
    public void fillRegisteredCount(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return;
        }

        List<Integer> ids = activities.stream()
                .map(Activity::getId)
                .toList();

        QueryWrapper<com.stu.huodongbm.entity.Registration> qw = new QueryWrapper<>();
        qw.select("activity_id", "COUNT(*) AS cnt")
                .in("status", List.of("registered", "approved"))
                .in("activity_id", ids)
                .groupBy("activity_id");

        List<Map<String, Object>> result = registrationMapper.selectMaps(qw);

        Map<Integer, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            Integer activityId = ((Number) row.get("activity_id")).intValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            countMap.put(activityId, cnt);
        }

        for (Activity a : activities) {
            a.setRegisteredCount(countMap.getOrDefault(a.getId(), 0));
        }
    }
}