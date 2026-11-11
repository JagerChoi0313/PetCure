package com.petcure.backend.service;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.petcure.backend.duo.user.UpdateUserProfileRequest;
import com.petcure.backend.duo.user.UserProfileResponse;
import com.petcure.backend.entity.USER;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.mapper.USERMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Service
public class UserService {
    private final USERMapper userMapper;

    public UserService(USERMapper userMapper){
        this.userMapper = userMapper;
    }

    public UserProfileResponse getCurrentUser(Long userId){
        USER user = userMapper.selectById(userId);

        if(user == null){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "用户不存在，请重新登录"
            );
        }

        //1.获取生日
        LocalDate birthday = user.getBirthday();

        //2.根据上海时区的当前日期计算周岁、
        Integer age = null;

        if(birthday != null){
            LocalDate today = LocalDate.now(
                    ZoneId.of("Asia/Shanghai")
            );

            age = Period.between(birthday,today).getYears();
        }

        //3.将数据库中的资料组织成接口返回对象
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar_url(),
                user.getGender(),
                birthday,
                age,
                user.getOccupation(),
                user.getCity(),
                user.getBio(),
                user.getPhone(),
                user.getEmail(),
                user.getCreated_at(),
                user.getLast_login_at()
        );
    }

    @Transactional
    public UserProfileResponse updateCurrentUser(
        Long userId,
        UpdateUserProfileRequest request
    ){
        //1.根据当前登录用户ID，只更新允许修改的资料字段
        int updateedRows = userMapper.update(
                null,
                new LambdaUpdateWrapper<USER>()
                        .eq(USER::getId,userId)
                        .set(USER::getNickname,request.getNickname())
        )
    }
}
