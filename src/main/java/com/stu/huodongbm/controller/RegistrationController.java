package com.stu.huodongbm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserService userService;

    @PostMapping("/join")
    public String join(@RequestParam Integer activityId, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            redirectAttributes.addFlashAttribute("error", "活动不存在");
            return "redirect:/activity/list";
        }

        if ("ended".equals(activity.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "活动已结束，无法报名");
            return "redirect:/activity/detail?id=" + activityId;
        }
        if ("ongoing".equals(activity.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "活动正在进行中，无法报名");
            return "redirect:/activity/detail?id=" + activityId;
        }
        if ("offline".equals(activity.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "活动已下线");
            return "redirect:/activity/detail?id=" + activityId;
        }

        if (activity.getDeadline() != null && activity.getDeadline().before(new java.util.Date())) {
            redirectAttributes.addFlashAttribute("error", "报名截止时间已过");
            return "redirect:/activity/detail?id=" + activityId;
        }

        LambdaQueryWrapper<Registration> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Registration::getUserId, user.getId());
        wrapper.eq(Registration::getActivityId, activityId);
        wrapper.in(Registration::getStatus, "registered", "approved");
        if (registrationService.count(wrapper) > 0) {
            redirectAttributes.addFlashAttribute("error", "您已报名该活动，请勿重复提交");
            return "redirect:/activity/detail?id=" + activityId;
        }

        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int currentApproved = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
            long pendingCount = registrationService.count(Wrappers.lambdaQuery(Registration.class)
                    .eq(Registration::getActivityId, activityId)
                    .eq(Registration::getStatus, "registered"));
            if (currentApproved + pendingCount >= activity.getMaxParticipants()) {
                redirectAttributes.addFlashAttribute("error", "报名名额已满");
                return "redirect:/activity/detail?id=" + activityId;
            }
        }

        Registration registration = new Registration();
        registration.setUserId(user.getId());
        registration.setActivityId(activityId);
        registration.setStatus("registered");
        registrationService.save(registration);

        redirectAttributes.addFlashAttribute("success", "报名成功，等待管理员审核");
        return "redirect:/activity/detail?id=" + activityId;
    }

    @GetMapping("/my")
    public String my(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        LambdaQueryWrapper<Registration> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Registration::getUserId, user.getId());
        wrapper.orderByDesc(Registration::getRegisterTime);
        List<Registration> registrationList = registrationService.list(wrapper);

        for (Registration reg : registrationList) {
            Activity activity = activityService.getById(reg.getActivityId());
            if (activity != null) {
                reg.setActivityTitle(activity.getTitle());
            }
        }

        model.addAttribute("registrationList", registrationList);
        return "registration/my";
    }

    @GetMapping("/cancel")
    public String cancel(@RequestParam Integer id, @RequestParam(required = false) Integer activityId, RedirectAttributes redirectAttributes) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "报名记录不存在");
            return "redirect:/registration/my";
        }

        if ("approved".equals(registration.getStatus())) {
            registration.setStatus("cancelled");
            registrationService.updateById(registration);

            Activity activity = activityService.getById(registration.getActivityId());
            if (activity != null) {
                int current = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
                if (current > 0) {
                    activity.setCurrentParticipants(current - 1);
                    activityService.updateById(activity);
                }
            }
            redirectAttributes.addFlashAttribute("success", "已取消报名");
        } else if ("registered".equals(registration.getStatus())) {
            registration.setStatus("cancelled");
            registrationService.updateById(registration);
            redirectAttributes.addFlashAttribute("success", "已撤销报名申请");
        } else {
            redirectAttributes.addFlashAttribute("error", "当前状态无法取消");
        }

        if (activityId != null) {
            return "redirect:/activity/detail?id=" + activityId;
        }
        return "redirect:/registration/my";
    }

    @PostMapping("/approve")
    public String approve(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "报名记录不存在");
            return "redirect:/registration/list";
        }

        if ("cancelled".equals(registration.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "已取消的报名无法重新审核");
            return "redirect:/registration/list";
        }

        Activity activity = activityService.getById(registration.getActivityId());
        if (activity != null && activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int current = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
            if (current >= activity.getMaxParticipants() && !"approved".equals(registration.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "该活动报名名额已满，无法通过审核");
                return "redirect:/registration/list";
            }
        }

        boolean needIncParticipants = !"approved".equals(registration.getStatus());

        registration.setStatus("approved");
        registrationService.updateById(registration);

        if (needIncParticipants && activity != null) {
            int current = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
            activity.setCurrentParticipants(current + 1);
            activityService.updateById(activity);
        }

        redirectAttributes.addFlashAttribute("success", "已通过审核");
        return "redirect:/registration/list";
    }

    @PostMapping("/reject")
    public String reject(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "报名记录不存在");
            return "redirect:/registration/list";
        }

        if ("cancelled".equals(registration.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "已取消的报名无法重新审核");
            return "redirect:/registration/list";
        }

        boolean needDecParticipants = "approved".equals(registration.getStatus());

        registration.setStatus("rejected");
        registrationService.updateById(registration);

        if (needDecParticipants) {
            Activity activity = activityService.getById(registration.getActivityId());
            if (activity != null) {
                int current = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
                if (current > 0) {
                    activity.setCurrentParticipants(current - 1);
                    activityService.updateById(activity);
                }
            }
        }

        redirectAttributes.addFlashAttribute("success", "已拒绝该报名");
        return "redirect:/registration/list";
    }

    @PostMapping("/reaudit")
    public String reaudit(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "报名记录不存在");
            return "redirect:/registration/list";
        }

        if ("cancelled".equals(registration.getStatus()) || "registered".equals(registration.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "当前状态无法重新审核");
            return "redirect:/registration/list";
        }

        if ("approved".equals(registration.getStatus())) {
            Activity activity = activityService.getById(registration.getActivityId());
            if (activity != null) {
                int current = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
                if (current > 0) {
                    activity.setCurrentParticipants(current - 1);
                    activityService.updateById(activity);
                }
            }
        }

        registration.setStatus("registered");
        registrationService.updateById(registration);

        redirectAttributes.addFlashAttribute("success", "已重置为待审核状态");
        return "redirect:/registration/list";
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Integer activityId, Model model) {
        LambdaQueryWrapper<Registration> wrapper = Wrappers.lambdaQuery();
        if (activityId != null) {
            wrapper.eq(Registration::getActivityId, activityId);
        }
        wrapper.orderByDesc(Registration::getRegisterTime);
        List<Registration> registrationList = registrationService.list(wrapper);

        for (Registration reg : registrationList) {
            User u = userService.getById(reg.getUserId());
            if (u != null) {
                reg.setUsername(u.getUsername());
                reg.setRealName(u.getRealName());
            }
            Activity a = activityService.getById(reg.getActivityId());
            if (a != null) {
                reg.setActivityTitle(a.getTitle());
            } else {
                reg.setActivityTitle("（活动已删除）");
            }
        }

        List<Activity> activityList = activityService.list(Wrappers.lambdaQuery(Activity.class).orderByDesc(Activity::getId));
        model.addAttribute("registrationList", registrationList);
        model.addAttribute("activityList", activityList);
        model.addAttribute("activityId", activityId);
        return "registration/list";
    }
}