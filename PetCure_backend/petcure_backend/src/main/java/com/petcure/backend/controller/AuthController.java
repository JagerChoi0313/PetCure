package com.petcure.backend.controller;

import com.petcure.backend.common.ApiResponse;
import com.petcure.backend.duo.auth.LoginRequest;
import com.petcure.backend.duo.auth.LoginResponse;
import com.petcure.backend.duo.auth.RegisterRequest;
import com.petcure.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    //Spring通过构造方法提供AuthService对象
    public AuthController(AuthService authService){
        this.authService=authService;
    }

    //接收POST /api/auth/register请求
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
            ){
        //调用注册业务：检查重复、生成密码哈希、保存用户
        authService.register(request);

        //注册完成，返回HTTP 201和统一返回结果
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
            ){
                LoginResponse response = authService.login(request);
                return ApiResponse.success(response);
                //类上已有@RequestMapping("/api/auth")，方法上新增@PostMapping("/login")
                //两者组合后POST http://localhost:8080/api/auth/login
                //这里的 @RequestBody 把请求中的 JSON 转成 LoginRequest；`@Valid` 检查用户名、密码等字段；
                //`authService.login(request)` 执行登录业务；最后通过 ApiResponse.success(...) 返回结果。
                //登录成功时，HTTP 状态默认是 200。
    }


    //在现有的AuthController中增加接口
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Jwt jwt){
        authService.logout(jwt);
        return ApiResponse.success();
    }
}
