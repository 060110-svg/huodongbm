package com.stu.huodongbm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    private String location;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date startTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date endTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date deadline;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String coverImage;
    private String status;
    private Date createTime;

    @TableField(exist = false)
    private Integer registeredCount;
}