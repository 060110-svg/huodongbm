package com.stu.huodongbm.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginUser implements Serializable {
    private Integer id;
    private String username;
    private String realName;
    private String role;

    public LoginUser() {
    }

    public LoginUser(Integer id, String username, String realName, String role) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.role = role;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isStudent() {
        return "student".equals(role);
    }
}