package com.petcure.backend.duo.user;


import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZoneId;

@Getter
@Setter
public class UpdateUserProfileRequest {

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @NotNull(message = "性别不能为空，未填写请选择0")
    @Min(value = 0,message = "性别只能为0、1、2")
    @Max(value = 2,message = "性别只能为0，1，2")
    private Integer gender;

    private LocalDate birthday;

    @Size(max = 100,message = "职位不能超过100个字符")
    private String occupation;

    @Size(max = 100,message = "城市不能超过100个字符")
    private String city;

    @Size(max = 500,message = "个人简介不能超过500个字符")
    private String bio;

    //在接收数据时，清理文本首尾空白
    public void setNickname(String nickname){
        this.nickname = normalize(nickname);
    }

    public void setOccupation(String occupation){
        this.occupation = normalize(occupation);
    }

    public void setCity(String city){
        this.city=normalize(city);
    }

    public void setBio(String bio){
        this.bio=normalize(bio);
    }

    //按上海时区的业务日期检查生日
    @AssertTrue(message = "生日不能晚于今天")
    public boolean isBirthdayValid(){
        if (birthday == null){
            return true;
        }

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Shanghai")
        );

        return !birthday.isAfter(today);
    }

    //未填写的文本统一转换为null
    private static String normalize(String value){
        if(value == null || value.isBlank()){
            return null;
        }

        return value.strip();
    }
}
