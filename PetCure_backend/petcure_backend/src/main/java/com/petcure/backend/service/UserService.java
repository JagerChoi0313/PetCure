package com.petcure.backend.service;


import com.petcure.backend.duo.user.UserProfileResponse;
import com.petcure.backend.entity.USER;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.mapper.USERMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar_url()
        );
    }
}
