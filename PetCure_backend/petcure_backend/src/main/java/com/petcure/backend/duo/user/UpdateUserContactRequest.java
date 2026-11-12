package com.petcure.backend.duo.user;
//修改联系方式，创建联系方式request

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Getter
@Setter
public class UpdateUserContactRequest {

    @NotBlank(message = "当前密码不能为空")
    @Size(max = 64,message = "当前密码不能超过64个字符")
    private String currentPassword;

    @Pattern(
            regexp = "^\\+[1-9][0-9]{6,14}$",
            message = "手机号格式不正确，请填写国家区号，例如+8613800138000"
    )
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 254,message = "邮箱不能超过254个字符")
    private String email;

    //手机号去除首尾空白，未填写时转换为null
    public void setPhone(String phone){
        this.phone = normalize(phone);
    }

    //邮箱去除首尾空白，并统一转换为小写
    public void setEmail(String Email){
        String normalizedEmail = normalize(email);

        this.email = normalizedEmail == null
                ?null
                :normalizedEmail.toLowerCase(Locale.ROOT);
    }

    //保存后的手机号和邮箱至少保留一项
    @AssertTrue(message = "手机号和邮箱至少填写一个")
    public boolean isContactProvided(){
        return phone != null || email != null;
    }

    //与现有登录接口的BCrypt输入限制保存一致
    @AssertTrue(message = "当前密码UTF-8编码后的长度不能超过72字节")
    public boolean isCurrentPasswordWithinByteLimit(){
        return currentPassword == null || currentPassword.getBytes(StandardCharsets.UTF_8).length<=72;
    }

    private static String normalize(String value){
        if (value == null || value.isBlank()){
            return null;
        }

        return value.strip();
    }
}
