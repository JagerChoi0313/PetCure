package com.petcure.backend.duo.user;
//创建返回对象

//USER表示数据库中的用户记录，包含密码哈希等内部数据
//UserProfileResponse表示接口允许返回的资料，因此不包含密码哈希，还可以包含数据库里没有计算的结果，例如年龄


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private final Long id;

    //登录账号
    private final String username;
    //昵称
    private final String nickname;
    //头像资源标识
    private final String avatarUrl;
    //0未填写，1男，2女
    private final Integer gender;
    //出生日期
    private final LocalDate birthday;
    //根据生日计算周岁，不保存到数据库
    private final Integer age;

    //职业
    private final String occupation;

    //所在城市
    private final String city;

    //个人简介
    private final String bio;

    //联系方式
    private final String phone;
    private final String email;

    //注册时间和最后登录时间，沿用数据库中的UTC时间
    private final LocalDateTime createdAt;
    private final LocalDateTime LastLoginAt;
}

//这是接口允许的返回的字段，不包括密码哈希。这里的avatarUrl是接口字段名，与数据库映射无关

