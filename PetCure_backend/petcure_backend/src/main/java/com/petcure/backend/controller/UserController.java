package com.petcure.backend.controller;

import com.petcure.backend.common.ApiResponse;
import com.petcure.backend.duo.user.UpdateUserContactRequest;
import com.petcure.backend.duo.user.UpdateUserPasswordRequest;
import com.petcure.backend.duo.user.UpdateUserProfileRequest;
import com.petcure.backend.duo.user.UserProfileResponse;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.service.UserService;
import jakarta.validation.Valid;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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


    //添加修改资料的接口
    @PutMapping("/me/profile")
    public ApiResponse<UserProfileResponse>updateProfile(
            @AuthenticationPrincipal Jwt jwt,   //获取Spring Security已经验证过的JWT
            @Valid @RequestBody UpdateUserProfileRequest request    //@Request：将请求体中的JSON转换为Request对象   @valid：执行Request中的字段校验和生日检查
            ){
        Long userId = getCurrentUserId(jwt);

        return ApiResponse.success(
                userService.updateCurrentUser(userId,request)
        );
    }

    //添加修改联系方式的接口
    @PutMapping("/me/contact")
    public ApiResponse<UserProfileResponse>updateContact(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserContactRequest request
            ){
        Long userId = getCurrentUserId(jwt);

        return ApiResponse.success(
                userService.updateCurrentUserContact(userId,request)
        );
    }

    private Long getCurrentUserId(Jwt jwt){
        try{
            Long userId = Long.valueOf(jwt.getSubject());

            if(userId <= 0){
                throw new NumberFormatException();
            }

            return userId;
        }catch(NumberFormatException exception){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "令牌中的用户ID无效，请重新登录"
            );
        }
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserPasswordRequest request
            ){
                Long userId = getCurrentUserId(jwt);

                //JwtConfig已经检查过版本号的类型、范围和有效性
                Number versionClaim = (Number)jwt.getClaims().get("token_version");
                Long tokenVersion = versionClaim.longValue();

                userService.updateCurrentUserPassword(
                        userId,
                        tokenVersion,
                        request
                );

                return ApiResponse.success();
    }
}
