package com.stu.huodongbm.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stu.huodongbm.common.RequireAdmin;
import com.stu.huodongbm.common.Result;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
public class ApiActivityController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiActivityController.class);

    @Autowired
    private ActivityService activityService;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
    private static final List<String> ALLOWED_EXT = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final int PAGE_SIZE = 9;

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

    private String validate(Activity activity) {
        if (activity.getTitle() == null || activity.getTitle().trim().isEmpty()) {
            return "活动标题不能为空";
        }
        if (activity.getTitle().trim().length() > 100) {
            return "活动标题不能超过100个字符";
        }
        if (activity.getLocation() != null && activity.getLocation().trim().length() > 100) {
            return "活动地点不能超过100个字符";
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            return "开始时间和结束时间不能为空";
        }
        if (!activity.getEndTime().after(activity.getStartTime())) {
            return "结束时间必须晚于开始时间";
        }
        if (activity.getDeadline() != null && !activity.getDeadline().before(activity.getStartTime())) {
            return "报名截止时间必须早于开始时间";
        }
        return null;
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String title,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(name = "page", defaultValue = "1") int page) {
        LambdaQueryWrapper<Activity> wrapper = Wrappers.lambdaQuery();
        if (title != null && !title.trim().isEmpty()) {
            wrapper.like(Activity::getTitle, title);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Activity::getStatus, status.trim());
        }
        wrapper.orderByDesc(Activity::getCreateTime).orderByDesc(Activity::getId);

        if (page < 1) {
            page = 1;
        }
        Page<Activity> pageResult = activityService.page(new Page<>(page, PAGE_SIZE), wrapper);
        if (pageResult.getPages() > 0 && page > pageResult.getPages()) {
            page = (int) pageResult.getPages();
            pageResult = activityService.page(new Page<>(page, PAGE_SIZE), wrapper);
        }

        List<Activity> activityList = pageResult.getRecords();
        activityService.fillRegisteredCount(activityList);

        int totalPages = (int) pageResult.getPages();
        int currentPage = (int) pageResult.getCurrent();
        Map<String, Object> data = new HashMap<>();
        data.put("list", activityList);
        data.put("total", pageResult.getTotal());
        data.put("currentPage", currentPage);
        data.put("totalPages", totalPages);
        data.put("pageSize", PAGE_SIZE);
        data.put("pageStart", Math.max(1, currentPage - 2));
        data.put("pageEnd", Math.min(totalPages, currentPage + 2));
        data.put("title", title);
        data.put("status", status);
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    public Result<Activity> detail(@PathVariable Integer id) {
        Activity activity = id != null ? activityService.getById(id) : null;
        if (activity == null) {
            return Result.fail(404, "活动不存在或已被删除");
        }
        activityService.fillRegisteredCount(List.of(activity));
        return Result.ok(activity);
    }

    @RequireAdmin
    @PostMapping
    public Result<Activity> save(Activity activity,
                                 @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        String err = validate(activity);
        if (err != null) {
            return Result.fail(400, err);
        }
        if (activity.getStatus() == null) {
            activity.setStatus("published");
        }
        if (activity.getCurrentParticipants() == null) {
            activity.setCurrentParticipants(0);
        }
        try {
            String coverPath = handleCoverUpload(coverFile);
            if (coverPath != null) {
                activity.setCoverImage(coverPath);
            }
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
        activityService.save(activity);
        return Result.ok(activity);
    }

    @RequireAdmin
    @PutMapping("/{id}")
    public Result<Activity> update(@PathVariable Integer id, Activity activity,
                                   @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        Activity existing = activityService.getById(id);
        if (existing == null) {
            return Result.fail(404, "活动不存在或已被删除");
        }
        activity.setId(id);
        String err = validate(activity);
        if (err != null) {
            return Result.fail(400, err);
        }
        try {
            String coverPath = handleCoverUpload(coverFile);
            if (coverPath != null) {
                activity.setCoverImage(coverPath);
            }
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
        activityService.update(activity, Wrappers.lambdaUpdate(Activity.class)
                .eq(Activity::getId, id)
                .set(activity.getDeadline() == null, Activity::getDeadline, null));
        return Result.ok(activityService.getById(id));
    }

    @RequireAdmin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        Activity activity = activityService.getById(id);
        activityService.removeById(id);
        // 报名记录由外键级联删除；封面文件无级联机制，需手动清理，避免 uploads/ 留下孤儿文件
        if (activity != null && activity.getCoverImage() != null
                && activity.getCoverImage().startsWith("/uploads/")) {
            File cover = new File(UPLOAD_DIR, activity.getCoverImage().substring("/uploads/".length()));
            if (cover.exists() && !cover.delete()) {
                log.warn("封面文件删除失败: {}", cover.getAbsolutePath());
            }
        }
        return Result.ok();
    }
}