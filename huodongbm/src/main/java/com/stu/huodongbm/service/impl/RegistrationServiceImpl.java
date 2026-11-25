package com.stu.huodongbm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.mapper.RegistrationMapper;
import com.stu.huodongbm.service.RegistrationService;
import org.springframework.stereotype.Service;

@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {
}