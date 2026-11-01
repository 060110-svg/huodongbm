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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserService userService;

    /**
     * 锁定活动行（SELECT ... FOR UPDATE），使同一活动上的并发报名/审核操作串行化。
     * 必须在事务内调用；活动不存在时返回 null。
     */
    private Activity lockActivity(Integer activityId) {
        return activityService.getOne(Wrappers.lambdaQuery(Activity.class)
                .eq(Activity::getId, activityId)
                .last("FOR UPDATE"));
    }

    /** 实时统计指定状态的报名人数（统一口径，不再依赖手工维护的 current_participants 做判定） */
    private int countByStatus(Integer activityId, String status) {
        return (int) registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, status));
    }

    /** 将 current_participants 重算同步为实际已通过人数（自愈，防止 ±1 漂移） */
    private void syncCurrentParticipants(Activity activity) {
        if (activity != null) {
            activity.setCurrentParticipants(countByStatus(activity.getId(), "approved"));
            activityService.updateById(activity);
        }
    }

    @PostMapping("/join")
    @Transactional(rollbackFor = Exception.class)
    public String join(@RequestParam Integer activityId, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!"student".equals(user.getRole())) {
            redirectAttributes.addFlashAttribute("error", "仅学生账号可以报名参加活动");
            return "redirect:/activity/list";
        }

        // 先锁定活动行：同一活动的并发报名在此排队，后续查重与名额检查在锁内原子完成
        Activity activity = lockActivity(activityId);
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

        // 查重（锁内）
        long existing = registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getUserId, user.getId())
                .eq(Registration::getActivityId, activityId)
                .in(Registration::getStatus, "registered", "approved"));
        if (existing > 0) {
            redirectAttributes.addFlashAttribute("error", "您已报名该活动，请勿重复提交");
            return "redirect:/activity/detail?id=" + activityId;
        }

        // 名额检查（锁内，实时 COUNT）
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int approvedCount = countByStatus(activityId, "approved");
            int pendingCount = countByStatus(activityId, "registered");
            if (approvedCount + pendingCount >= activity.getMaxParticipants()) {
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

        // 批量取活动标题，避免逐条查询（N+1）
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

        model.addAttribute("registrationList", registrationList);
        return "registration/my";
    }

    @PostMapping("/cancel")
    @Transactional(rollbackFor = Exception.class)
    public String cancel(@RequestParam Integer id, @RequestParam(required = false) Integer activityId, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("user");
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "报名记录不存在");
            return "redirect:/registration/my";
        }

        if (currentUser == null || !registration.getUserId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权操作他人的报名记录");
            return "redirect:/registration/my";
        }

        if ("approved".equals(registration.getStatus())) {
            // 先锁活动行，再变更状态，最后按实际 approved 数重算人数
            Activity activity = lockActivity(registration.getActivityId());
            registration.setStatus("cancelled");
            registrationService.updateById(registration);
            syncCurrentParticipants(activity);
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
    @Transactional(rollbackFor = Exception.class)
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

        // 锁定活动行：审核与报名、审核与审核之间的并发在此串行化
        Activity activity = lockActivity(registration.getActivityId());
        boolean alreadyApproved = "approved".equals(registration.getStatus());
        if (!alreadyApproved && activity != null
                && activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int approvedCount = countByStatus(activity.getId(), "approved");
            if (approvedCount >= activity.getMaxParticipants()) {
                redirectAttributes.addFlashAttribute("error", "该活动报名名额已满，无法通过审核");
                return "redirect:/registration/list";
            }
        }

        registration.setStatus("approved");
        registrationService.updateById(registration);
        syncCurrentParticipants(activity);

        redirectAttributes.addFlashAttribute("success", "已通过审核");
        return "redirect:/registration/list";
    }

    @PostMapping("/reject")
    @Transactional(rollbackFor = Exception.class)
    public String reject(@RequestParam Integer id, @RequestParam(required = false) String remark, RedirectAttributes redirectAttributes) {
        if (remark != null && remark.trim().length() > 255) {
            redirectAttributes.addFlashAttribute("error", "拒绝原因不能超过255个字符");
            return "redirect:/registration/list";
        }

        Registration registration = registrationService.getById(id);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "报名记录不存在");
            return "redirect:/registration/list";
        }

        if ("cancelled".equals(registration.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "已取消的报名无法重新审核");
            return "redirect:/registration/list";
        }

        // 无条件锁定活动行并重算人数：消除与 approve 并发时基于旧读跳过重算导致的计数漂移
        Activity activity = lockActivity(registration.getActivityId());

        registration.setStatus("rejected");
        // 写入拒绝原因（空值写 "" 以清除上一次审核的残留备注）
        registration.setRemark(remark == null ? "" : remark.trim());
        registrationService.updateById(registration);

        syncCurrentParticipants(activity);

        redirectAttributes.addFlashAttribute("success", "已拒绝该报名");
        return "redirect:/registration/list";
    }

    @PostMapping("/reaudit")
    @Transactional(rollbackFor = Exception.class)
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

        // 无条件锁定活动行并重算人数：消除与 approve 并发时基于旧读跳过重算导致的计数漂移
        Activity activity = lockActivity(registration.getActivityId());

        registration.setStatus("registered");
        registration.setRemark("");
        registrationService.updateById(registration);

        syncCurrentParticipants(activity);

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

        // 批量取用户与活动信息，避免逐条查询（N+1）
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

        List<Activity> activityList = activityService.list(Wrappers.lambdaQuery(Activity.class).orderByDesc(Activity::getId));
        model.addAttribute("registrationList", registrationList);
        model.addAttribute("activityList", activityList);
        model.addAttribute("activityId", activityId);
        return "registration/list";
    }
}
