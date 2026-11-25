package com.stu.huodongbm.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stu.huodongbm.common.LoginUser;
import com.stu.huodongbm.common.RequireAdmin;
import com.stu.huodongbm.common.Result;
import com.stu.huodongbm.common.UserContext;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.service.ActivityService;
import com.stu.huodongbm.service.RegistrationService;
import com.stu.huodongbm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
public class ApiRegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserService userService;

    private Activity lockActivity(Integer activityId) {
        return activityService.getOne(Wrappers.lambdaQuery(Activity.class)
                .eq(Activity::getId, activityId)
                .last("FOR UPDATE"));
    }

    private int countByStatus(Integer activityId, String status) {
        return (int) registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, status));
    }

    private void syncCurrentParticipants(Activity activity) {
        if (activity != null) {
            activity.setCurrentParticipants(countByStatus(activity.getId(), "approved"));
            activityService.updateById(activity);
        }
    }

    @PostMapping("/join")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> join(@RequestParam Integer activityId) {
        LoginUser user = UserContext.get();
        if (user == null) {
            return Result.fail(401, "未登录");
        }
        if (!"student".equals(user.getRole())) {
            return Result.fail(403, "仅学生账号可以报名参加活动");
        }

        Activity activity = lockActivity(activityId);
        if (activity == null) {
            return Result.fail(404, "活动不存在");
        }
        if ("ended".equals(activity.getStatus())) {
            return Result.fail(400, "活动已结束，无法报名");
        }
        if ("ongoing".equals(activity.getStatus())) {
            return Result.fail(400, "活动正在进行中，无法报名");
        }
        if ("offline".equals(activity.getStatus())) {
            return Result.fail(400, "活动已下线");
        }
        if (activity.getDeadline() != null && activity.getDeadline().before(new Date())) {
            return Result.fail(400, "报名截止时间已过");
        }

        long existing = registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getUserId, user.getId())
                .eq(Registration::getActivityId, activityId)
                .in(Registration::getStatus, "registered", "approved"));
        if (existing > 0) {
            return Result.fail(400, "您已报名该活动，请勿重复提交");
        }

        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int approvedCount = countByStatus(activityId, "approved");
            int pendingCount = countByStatus(activityId, "registered");
            if (approvedCount + pendingCount >= activity.getMaxParticipants()) {
                return Result.fail(400, "报名名额已满");
            }
        }

        Registration registration = new Registration();
        registration.setUserId(user.getId());
        registration.setActivityId(activityId);
        registration.setStatus("registered");
        registrationService.save(registration);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<List<Registration>> my() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return Result.fail(401, "未登录");
        }
        LambdaQueryWrapper<Registration> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Registration::getUserId, user.getId());
        wrapper.orderByDesc(Registration::getRegisterTime);
        List<Registration> registrationList = registrationService.list(wrapper);

        if (!registrationList.isEmpty()) {
            List<Integer> activityIds = registrationList.stream()
                    .map(Registration::getActivityId).distinct().toList();
            Map<Integer, String> titleMap = new HashMap<>();
            for (Activity a : activityService.listByIds(activityIds)) {
                titleMap.put(a.getId(), a.getTitle());
            }
            for (Registration reg : registrationList) {
                reg.setActivityTitle(titleMap.get(reg.getActivityId()));
            }
        }
        return Result.ok(registrationList);
    }

    @PostMapping("/{id}/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(@PathVariable Integer id) {
        LoginUser currentUser = UserContext.get();
        if (currentUser == null) {
            return Result.fail(401, "未登录");
        }
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.fail(404, "报名记录不存在");
        }
        if (!registration.getUserId().equals(currentUser.getId())) {
            return Result.fail(403, "无权操作他人的报名记录");
        }

        if ("approved".equals(registration.getStatus())) {
            Activity activity = lockActivity(registration.getActivityId());
            registration.setStatus("cancelled");
            registrationService.updateById(registration);
            syncCurrentParticipants(activity);
            return Result.ok();
        } else if ("registered".equals(registration.getStatus())) {
            registration.setStatus("cancelled");
            registrationService.updateById(registration);
            return Result.ok();
        } else {
            return Result.fail(400, "当前状态无法取消");
        }
    }

    @RequireAdmin
    @PostMapping("/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> approve(@PathVariable Integer id) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.fail(404, "报名记录不存在");
        }
        if ("cancelled".equals(registration.getStatus())) {
            return Result.fail(400, "已取消的报名无法重新审核");
        }

        Activity activity = lockActivity(registration.getActivityId());
        boolean alreadyApproved = "approved".equals(registration.getStatus());
        if (!alreadyApproved && activity != null
                && activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int approvedCount = countByStatus(activity.getId(), "approved");
            if (approvedCount >= activity.getMaxParticipants()) {
                return Result.fail(400, "该活动报名名额已满，无法通过审核");
            }
        }

        registration.setStatus("approved");
        registrationService.updateById(registration);
        syncCurrentParticipants(activity);
        return Result.ok();
    }

    @RequireAdmin
    @PostMapping("/{id}/reject")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reject(@PathVariable Integer id,
                               @RequestParam(required = false) String remark) {
        if (remark != null && remark.trim().length() > 255) {
            return Result.fail(400, "拒绝原因不能超过255个字符");
        }
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.fail(404, "报名记录不存在");
        }
        if ("cancelled".equals(registration.getStatus())) {
            return Result.fail(400, "已取消的报名无法重新审核");
        }

        Activity activity = lockActivity(registration.getActivityId());
        registration.setStatus("rejected");
        registration.setRemark(remark == null ? "" : remark.trim());
        registrationService.updateById(registration);
        syncCurrentParticipants(activity);
        return Result.ok();
    }

    @RequireAdmin
    @PostMapping("/{id}/reaudit")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reaudit(@PathVariable Integer id) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.fail(404, "报名记录不存在");
        }
        if ("cancelled".equals(registration.getStatus()) || "registered".equals(registration.getStatus())) {
            return Result.fail(400, "当前状态无法重新审核");
        }

        Activity activity = lockActivity(registration.getActivityId());
        registration.setStatus("registered");
        registration.setRemark("");
        registrationService.updateById(registration);
        syncCurrentParticipants(activity);
        return Result.ok();
    }

    @RequireAdmin
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) Integer activityId) {
        LambdaQueryWrapper<Registration> wrapper = Wrappers.lambdaQuery();
        if (activityId != null) {
            wrapper.eq(Registration::getActivityId, activityId);
        }
        wrapper.orderByDesc(Registration::getRegisterTime);
        List<Registration> registrationList = registrationService.list(wrapper);

        if (!registrationList.isEmpty()) {
            List<Integer> userIds = registrationList.stream()
                    .map(Registration::getUserId).distinct().toList();
            List<Integer> activityIds = registrationList.stream()
                    .map(Registration::getActivityId).distinct().toList();
            Map<Integer, User> userMap = new HashMap<>();
            for (User u : userService.listByIds(userIds)) {
                userMap.put(u.getId(), u);
            }
            Map<Integer, String> titleMap = new HashMap<>();
            for (Activity a : activityService.listByIds(activityIds)) {
                titleMap.put(a.getId(), a.getTitle());
            }
            for (Registration reg : registrationList) {
                User u = userMap.get(reg.getUserId());
                if (u != null) {
                    reg.setUsername(u.getUsername());
                    reg.setRealName(u.getRealName());
                }
                String t = titleMap.get(reg.getActivityId());
                reg.setActivityTitle(t != null ? t : "（活动已删除）");
            }
        }

        List<Activity> activityList = activityService.list(
                Wrappers.lambdaQuery(Activity.class).orderByDesc(Activity::getId));

        Map<String, Object> data = new HashMap<>();
        data.put("list", registrationList);
        data.put("activities", activityList);
        data.put("activityId", activityId);
        return Result.ok(data);
    }
}