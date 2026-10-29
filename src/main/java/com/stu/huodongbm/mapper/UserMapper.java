package com.stu.huodongbm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stu.huodongbm.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {//用户Mapper接口
}