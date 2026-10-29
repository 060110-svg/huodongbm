package com.stu.huodongbm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("registration")
public class Registration {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer activityId;
    private Date registerTime;
    private String status;
    private String remark;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String realName;

    @TableField(exist = false)
    private String activityTitle;
}