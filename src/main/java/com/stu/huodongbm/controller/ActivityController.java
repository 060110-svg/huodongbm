package com.stu.huodongbm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.service.ActivityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
    private static final List<String> ALLOWED_EXT = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final int PAGE_SIZE = 9;

    /**
     * 处理封面上传。未选择文件返回 null（正常路径）；
     * 格式不符 / 超过大小 / 写入失败时抛出带用户提示的异常，不再静默吞掉。
     */
    private String handleCoverUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String originalName = file.getOriginalFilename();
        String ext = originalName == null ? ""
                : originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("封面图仅支持 JPG / JPEG / PNG / GIF / WEBP 格式");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("封面图大小不能超过 10MB");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest);
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new IllegalArgumentException("封面图保存失败，请稍后重试");
        }
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String title,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        LambdaQueryWrapper<Activity> wrapper = Wrappers.lambdaQuery();
        if (title != null && !title.trim().isEmpty()) {
            wrapper.like(Activity::getTitle, title);
        }
        wrapper.orderByDesc(Activity::getCreateTime).orderByDesc(Activity::getId);

        if (page < 1) {
            page = 1;
        }
        Page<Activity> pageResult = activityService.page(new Page<>(page, PAGE_SIZE), wrapper);

        // 页码越界：重定向回末页（保留搜索词），避免误导性的空态与异常页码窗口
        if (pageResult.getPages() > 0 && page > pageResult.getPages()) {
            redirectAttributes.addAttribute("title", title);
            redirectAttributes.addAttribute("page", (int) pageResult.getPages());
            return "redirect:/activity/list";
        }

        List<Activity> activityList = pageResult.getRecords();
        activityService.fillRegisteredCount(activityList);

        model.addAttribute("activityList", activityList);
        model.addAttribute("title", title);
        model.addAttribute("totalCount", pageResult.getTotal());
        int totalPages = (int) pageResult.getPages();
        int currentPage = (int) pageResult.getCurrent();
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage);
        // 页码窗口在控制器计算：Thymeleaf 3.1 的 SpEL ACL 禁用 T(Math)
        model.addAttribute("pageStart", Math.max(1, currentPage - 2));
        model.addAttribute("pageEnd", Math.min(totalPages, currentPage + 2));
        model.addAttribute("user", session.getAttribute("user"));
        return "activity/list";
    }

    @GetMapping("/detail")
    public String detail(Integer id, Model model, HttpSession session) {
        Activity activity = id != null ? activityService.getById(id) : null;
        if (activity != null) {
            activityService.fillRegisteredCount(List.of(activity));
        }
        model.addAttribute("activity", activity);
        model.addAttribute("user", session.getAttribute("user"));
        return "activity/detail";
    }

    @GetMapping("/toAdd")
    public String toAdd(HttpSession session, Model model) {
        model.addAttribute("user", session.getAttribute("user"));
        return "activity/add";
    }

    @PostMapping("/save")
    public String save(Activity activity, @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                       RedirectAttributes redirectAttributes) {
        // 服务端校验：数据库 start_time/end_time 为 NOT NULL，缺失会导致 SQL 异常 500
        if (activity.getTitle() == null || activity.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "活动标题不能为空");
            return "redirect:/activity/toAdd";
        }
        if (activity.getTitle().trim().length() > 100) {
            redirectAttributes.addFlashAttribute("error", "活动标题不能超过100个字符");
            return "redirect:/activity/toAdd";
        }
        if (activity.getLocation() != null && activity.getLocation().trim().length() > 100) {
            redirectAttributes.addFlashAttribute("error", "活动地点不能超过100个字符");
            return "redirect:/activity/toAdd";
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            redirectAttributes.addFlashAttribute("error", "开始时间和结束时间不能为空");
            return "redirect:/activity/toAdd";
        }
        if (activity.getStatus() == null) {
            activity.setStatus("published");
        }
        if (activity.getCurrentParticipants() == null) {
            activity.setCurrentParticipants(0);
        }
        String coverPath;
        try {
            coverPath = handleCoverUpload(coverFile);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activity/toAdd";
        }
        if (coverPath != null) {
            activity.setCoverImage(coverPath);
        }
        activityService.save(activity);
        return "redirect:/activity/list";
    }

    @GetMapping("/toEdit")
    public String toEdit(Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Activity activity = id != null ? activityService.getById(id) : null;
        if (activity == null) {
            redirectAttributes.addFlashAttribute("error", "活动不存在或已被删除");
            return "redirect:/activity/list";
        }
        model.addAttribute("activity", activity);
        model.addAttribute("user", session.getAttribute("user"));
        return "activity/edit";
    }

    @PostMapping("/update")
    public String update(Activity activity, @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                         RedirectAttributes redirectAttributes) {
        if (activity.getId() == null) {
            redirectAttributes.addFlashAttribute("error", "活动ID无效");
            return "redirect:/activity/list";
        }
        if (activity.getTitle() == null || activity.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "活动标题不能为空");
            return "redirect:/activity/toEdit?id=" + activity.getId();
        }
        if (activity.getTitle().trim().length() > 100) {
            redirectAttributes.addFlashAttribute("error", "活动标题不能超过100个字符");
            return "redirect:/activity/toEdit?id=" + activity.getId();
        }
        if (activity.getLocation() != null && activity.getLocation().trim().length() > 100) {
            redirectAttributes.addFlashAttribute("error", "活动地点不能超过100个字符");
            return "redirect:/activity/toEdit?id=" + activity.getId();
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            redirectAttributes.addFlashAttribute("error", "开始时间和结束时间不能为空");
            return "redirect:/activity/toEdit?id=" + activity.getId();
        }
        String coverPath;
        try {
            coverPath = handleCoverUpload(coverFile);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/activity/toEdit?id=" + activity.getId();
        }
        if (coverPath != null) {
            activity.setCoverImage(coverPath);
        }
        // updateById 默认忽略 null 字段，报名截止被清空时需显式置 null 才能生效
        activityService.update(activity, Wrappers.lambdaUpdate(Activity.class)
                .eq(Activity::getId, activity.getId())
                .set(activity.getDeadline() == null, Activity::getDeadline, null));
        return "redirect:/activity/list";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Integer id) {
        activityService.removeById(id);
        return "redirect:/activity/list";
    }
}