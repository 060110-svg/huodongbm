package com.stu.huodongbm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.mapper.UserMapper;
import com.stu.huodongbm.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}