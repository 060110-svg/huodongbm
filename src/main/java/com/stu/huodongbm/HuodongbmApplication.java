package com.stu.huodongbm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.stu.huodongbm.mapper")
public class HuodongbmApplication {
    public static void main(String[] args) {
        SpringApplication.run(HuodongbmApplication.class, args);
    }
}