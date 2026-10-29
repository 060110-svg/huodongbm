package com.stu.huodongbm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stu.huodongbm.entity.Activity;

import java.util.List;
import java.util.Map;

public interface ActivityService extends IService<Activity> {
    void fillRegisteredCount(List<Activity> activities);
}