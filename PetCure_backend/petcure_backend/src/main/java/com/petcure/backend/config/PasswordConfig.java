package com.petcure.backend.config;//创建密码配置类

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration  //告诉Spring，这是一个配置类
public class PasswordConfig {

    @Bean   //让Spring调用这个返回方法，并管理它返回的对象
    public PasswordEncoder passwordEncoder(){   //我们配置的是一个PasswordEncorder，具体使用BCryptPasswordEncoder实现。
        return new BCryptPasswordEncoder();
    }
}
//后面调用passwordEncoder.encode(password);就可以得到密码哈希。登录时则通过matches()检查密码