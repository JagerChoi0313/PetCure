package com.petcure.backend.duo.user;
//创建返回对象

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private final Long id;
    private final String username;
    private final String nickname;
    private final String avatarUrl;
}

//这是接口允许的返回的字段，不包括密码哈希。这里的avatarUrl是接口字段名，与数据库映射无关

