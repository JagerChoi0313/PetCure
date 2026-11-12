package com.petcure.backend.duo.auth;
//duo用于存放接口传输的数据对象,auth表示认证相关功能
//RegisterRequest专门接收前端注册时提交的用户名、密码、确认密码、手机号和邮箱，并且检查数据是否符合要求

//这是项目中的一个类，接收前端传过来的注册信息
//为什么需要这个，因为前端注册时会发过来一些数据
//如:{
//    "username": "tom123",
//    "password": "123456",
//    "phone": "13800000000",
//    "email": "tom@qq.com"
//}
//Spring Boot会把这些JSON数据转换成一个Java对象，这个Java对象就是RegisterRequest

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Locale;


@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4,max = 50,message="用户名长度必须为4到50位")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "用户名只能包含字母、数字和下划线"
            //它的含义是：
            //- a-zA-Z：允许英文字母。
            //- 0-9：允许数字。
            //- _：允许下划线。
            //- +：允许前面的字符连续出现一次或多次。
            //- ^、`$`：要求整个用户名都符合规则。
    )
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min=8, max = 64,message = "密码长度必须为8到64位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Size(min=8, max = 64,message = "确认密码长度不能超过64位")
    private String confirmPassword;

    @Pattern(
            regexp = "^\\+[1-9][0-9]{6,14}$",
            message = "手机号格式不正确，请填写国家区号，例如 +8613800138000"
    )
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message="邮箱长度不能超过254个字符")
    private String email;

    //清理用户名首尾空白
    public void setUsername(String username){
        this.username = normalize(username);
    }

    //未填写的手机号统一转换为null
    public void setPhone(String phone){
        this.phone = normalize(phone);
    }

    //未填写的邮箱统一转换为null
    public void setEmail(String email){

        String normalizedEmail = normalize(email);

        this.email = normalizedEmail == null
                ?null
                :normalizedEmail.toLowerCase(Locale.ROOT);
    }

    //手机号、邮箱至少填一个
    @AssertTrue(message = "手机号和邮箱至少填一个")
    public boolean isContactProvided(){
        return phone != null || email != null;
    }

    @AssertTrue(message = "两次输入的密码不一致")
    public boolean isPasswordConfirmed(){
        if(password == null || confirmPassword == null){
            //缺失情况交给字段上的@BotBlank处理
            return true;
        }
        return password.equals(confirmPassword);
    }

    //检查BCrypt允许的密码字节长度
    @AssertTrue(message = "密码UTF-8编码后的长度不能超过72字节")
    public boolean isPasswordWithinByteLimit(){
        return password == null
                || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static String normalize(String value){
        if(value==null || value.isBlank()){
            return null;
        }
        return value.strip();
    }
}




