package com.petcure.backend.duo.auth;

//登录只需要提交两个字段username和password

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message="用户名不能为空")
    @Size(max=50,message = "用户名长度不能超过50位")
    private String username;

    @NotBlank(message="密码不能为空")
    @Size(max = 64,message = "密码长度不能超过64位")
    private String password;

    //清理用户名首尾空白
    public void setUsername(String username){
        if(username==null){
            this.username = null;
            return;
        }
        this.username = username.strip();//清理用户名首尾空白
    }

    public void setPassword(String password) {
        this.password = password;
    }

    //与注册时使用的BCrypt密码长度限制保持一致
    @AssertTrue(message = "密码UTF-8编码后的长度不能超过72字节")
    public boolean isPasswordWithinByteLimit(){
        return password == null || password.getBytes(StandardCharsets.UTF_8).length<=72;
    }

}
