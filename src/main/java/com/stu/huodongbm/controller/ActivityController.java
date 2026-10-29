package com.stu.huodongbm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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

    private String handleCoverUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return null;
        }
        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            return null;
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return null;
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
            return null;
        }
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String title,
                       Model model, HttpSession session) {
        LambdaQueryWrapper<Activity> wrapper = Wrappers.lambdaQuery();
        if (title != null && !title.trim().isEmpty()) {
            wrapper.like(Activity::getTitle, title);
        }
        wrapper.orderByDesc(Activity::getCreateTime).orderByDesc(Activity::getId);

        List<Activity> activityList = activityService.list(wrapper);
        activityService.fillRegisteredCount(activityList);

        model.addAttribute("activityList", activityList);
        model.addAttribute("title", title);
        model.addAttribute("totalCount", activityList.size());
        model.addAttribute("user", session.getAttribute("user"));
        return "activity/list";
    }

    @GetMapping("/detail")
    public String detail(Integer id, Model model, HttpSession session) {
        Activity activity = activityService.getById(id);
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
    public String save(Activity activity, @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        if (activity.getStatus() == null) {
            activity.setStatus("published");
        }
        if (activity.getCurrentParticipants() == null) {
            activity.setCurrentParticipants(0);
        }
        String coverPath = handleCoverUpload(coverFile);
        if (coverPath != null) {
            activity.setCoverImage(coverPath);
        }
        activityService.save(activity);
        return "redirect:/activity/list";
    }

    @GetMapping("/toEdit")
    public String toEdit(Integer id, Model model, HttpSession session) {
        model.addAttribute("activity", activityService.getById(id));
        model.addAttribute("user", session.getAttribute("user"));
        return "activity/edit";
    }

    @PostMapping("/update")
    public String update(Activity activity, @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        String coverPath = handleCoverUpload(coverFile);
        if (coverPath != null) {
            activity.setCoverImage(coverPath);
        }
        activityService.updateById(activity);
        return "redirect:/activity/list";
    }

    @GetMapping("/delete")
    public String delete(Integer id) {
        activityService.removeById(id);
        return "redirect:/activity/list";
    }
}