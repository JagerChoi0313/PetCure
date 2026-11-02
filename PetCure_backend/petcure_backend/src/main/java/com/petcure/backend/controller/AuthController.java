package com.petcure.backend.controller;

import com.petcure.backend.common.ApiResponse;
import com.petcure.backend.duo.auth.RegisterRequest;
import com.petcure.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
