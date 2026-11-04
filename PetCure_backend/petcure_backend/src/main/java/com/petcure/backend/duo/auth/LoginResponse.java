package com.petcure.backend.duo.auth;
//创建登录返回对象


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class LoginResponse {

    //前端后续请求携带的令牌
    private final String accessToken;

    //令牌类型：固定为Bearer
    private final String tokenType;

    //令牌到期时间
    private final Instant expiresAt;
}
//用于规定“登录成功以后返回哪些数据”。我们不直接返回USER,避免把密码哈希等字段发送给前端
//@AllArgsConstructor会生成一个接收全部字段的构造方法，因此后面可以这样创建对象
//new LoginResponse(token, "Bearer", expiresAt);