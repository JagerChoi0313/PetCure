package com.petcure.backend.duo.user;


import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class UpdateUserPasswordRequest {
    @NotBlank(message = "当前密码不能为空")
    @Size(max=64,message = "当前密码不能超过64个字符")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8,max = 64,message = "新密码长度必须为8到64个字符")
    @Pattern(
            regexp = "(?s)^(?=.*[A-Za-z])(?=.*[0-9]).*$",
            message = "新密码必须同时包含英文字母和数字"
    )
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(max = 64,message = "确认密码不能超过64个字符")
    private String confirmPassword;

    //新密码与确认密码必须一致
    @AssertTrue(message = "两次输入的密码并不一致")
    public boolean isPasswordConfirmed(){
        if (newPassword == null || confirmPassword == null){
            return true;
        }

        return newPassword.equals(confirmPassword);
    }

    //当前密码的检查方法
    @AssertTrue(message = "当前密码UTF-8编码后的长度不能超过72字节")
    public boolean isCurrentPasswordWithinByteLimit(){
        return currentPassword == null || currentPassword.getBytes(StandardCharsets.UTF_8).length<=72;
    }


    //当前密码必须符合BCryPt的输入长度限制
    @AssertTrue(message = "新密码UTF-8编码后的长度不能超过72字节")
    public boolean isNewPasswordWithinByteLimit(){
        return newPassword == null|| newPassword.getBytes(StandardCharsets.UTF_8).length<=72;
    }
}
