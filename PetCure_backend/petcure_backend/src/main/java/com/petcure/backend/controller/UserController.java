package com.petcure.backend.controller;

import com.petcure.backend.common.ApiResponse;
import com.petcure.backend.duo.user.UserProfileResponse;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.service.UserService;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse>me(
            @AuthenticationPrincipal Jwt jwt    //获取的是 Spring Security 已经完成签名、有效期等检查的 JWT。
            ){
                //接口行为没有改变，只是吧读取ID的代码移动到公共方法中
                Long userId = getCurrentUserId(jwt);

                return ApiResponse.success(
                        userService.getCurrentUser(userId)
                );
    }

    private Long getCurrentUserId(Jwt jwt){
        try{
            Long userId = Long.valueOf(jwt.getSubject());

            if(userId <= 0){
                throw new NumberFormatException();
            }

            return userId;
        }catch(NumberFormatException){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "令牌中的用户ID无效，请重新登录"
            );
        }
    }
}
