package com.petcure.backend.service;


import com.petcure.backend.duo.auth.RegisterRequest;
import com.petcure.backend.mapper.USERMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final USERMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
                USERMapper userMapper,
                PasswordEncoder passwordEncoder
    ){
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public void register(RegisterRequest request){
        String username = request.getUsername();
        String phone = request.getPhone();
        String email = request.getEmail();

        //检查用户名是否重复
        //今天复习概率论去了，傻逼概率论，老子昨晚才知道周一要考试
    }



}
