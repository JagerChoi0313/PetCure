package com.petcure.backend.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//Lambda是Mybatis-Plus提供的一个工具类，它的作用是帮助我们构建数据库查询条件
//比如我们想查询：select * From user WHERE username = 'tom123';
//可以使用代码new LambdaQueryWrapper<USER>()  .eq(USER::getUsername,"tom123")
//因此我们不需要自己写完整的SQL，就可以通过Java代码构建查询条件
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.petcure.backend.duo.auth.LoginRequest;
import com.petcure.backend.duo.auth.LoginResponse;
import com.petcure.backend.duo.auth.RegisterRequest;
//这是项目中的一个类，接收前端传过来的注册信息
//为什么需要这个，因为前端注册时会发过来一些数据
//如:{
//    "username": "tom123",
//    "password": "123456",
//    "phone": "13800000000",
//    "email": "tom@qq.com"
//}
//Spring Boot会把这些JSON数据转换成一个Java对象，这个Java对象就是RegisterRequest
import com.petcure.backend.entity.USER;
//这是项目中的用户实体类，表示数据中的一条用户记录（表示需要保存到数据库的用户数据）
import com.petcure.backend.exception.BusinessException;
//业务异常类，作用是当用户违反业务规则时，主动抛出异常，例如用户名已经存在，手机号已经被使用
//为什么不使用Java自带的异常，因为我们希望自己控制返回给前端的信息
import com.petcure.backend.mapper.JwtRevocationMapper;
import com.petcure.backend.mapper.USERMapper;
//这是MyBatis-plus对应的数据库操作接口
//USER：描述用户数据，USERMapper:负责操作数据库中的用户数据
import org.springframework.http.HttpStatus;//这是Spring提供的HTTP状态码类
//例如HTTPStatus.CONFLICT代表HTTP 409，意思是：请求于当前服务器资源状态发生冲突（就比如用户名已被注册）
import org.springframework.security.crypto.password.PasswordEncoder;//这是Spring提供的密码处理接口
//它主要负责对密码进行哈希处理，验证用户输入的密码是否正确
//例如passwordEncoder.encode("123456");将原始密码处理成哈希值。
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;


//这个注解用于标记业务层组件，意味着Spring自动管理这个类，并且可以把它注入到其他需要使用它的类中
@Service
public class AuthService {//身份认证业务服务类
    private final USERMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;//让AuthService接收JwtService
    private final JwtRevocationMapper jwtRevocationMapper;

    public AuthService(
                USERMapper userMapper,
                PasswordEncoder passwordEncoder,
                JwtService jwtService,
                JwtRevocationMapper jwtRevocationMapper
    ){
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtService=jwtService;
        this.jwtRevocationMapper = jwtRevocationMapper;
    }

    public void logout(Jwt jwt){
        Object issuer = jwt.getClaims().get(JwtClaimNames.ISS);
        String jti = jwt.getId();


        if(issuer == null || jti == null || jti.isBlank()
                || jwt.getExpiresAt()==null){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "令牌缺少必要信息，请重新登录"
            );
        }

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                jwt.getExpiresAt(),
                ZoneOffset.UTC
        );

        jwtRevocationMapper.insertRevocation(issuer.toString(),jti,expiresAt);
    }



    public LoginResponse login(LoginRequest request){

        //1.根据用户名查询用户
        USER user = userMapper.selectOne(
                new LambdaQueryWrapper<USER>()
                        .eq(USER::getUsername,request.getUsername())
        );

        //2.用户不存在，或者密码不正确，都返回相同提示
        if(user == null || !passwordEncoder.matches(
                request.getPassword(),
                user.getPassword_hash()
        )){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "用户名或密码错误"
            );
        }

        //3.身份验证通过，生成JWT
        Jwt jwt = jwtService.createAccessToken(user.getId(),user.getToken_version());

        //4.更新最后登录时间，使用UTC与数据库时间保持一致
        userMapper.update(
                null,
                new LambdaUpdateWrapper<USER>()
                        .eq(USER::getId,user.getId())
                        .set(
                                USER::getLast_login_at,
                                LocalDateTime.now(ZoneOffset.UTC)
                        )
        );

        //返回登录结果
        return new LoginResponse(
                jwt.getTokenValue(),
                "Bearer",
                jwt.getExpiresAt()
        );
    }

    public void register(RegisterRequest request){
        String username = request.getUsername();
        String phone = request.getPhone();
        String email = request.getEmail();

        //1，检查用户名是否重复
        boolean usernameExists = userMapper.selectCount(
                new LambdaQueryWrapper<USER>()
                        .eq(USER::getUsername,username)
        )>0;

        if (usernameExists){
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "USERNAME_EXIST",
                    "用户名已被使用"
            );
        }

        //2.如果写了手机号，检查是否重复
        if(phone != null && !phone.isBlank()){
            boolean phoneExists = userMapper.selectCount(
                    new LambdaQueryWrapper<USER>()
                            .eq(USER::getPhone,phone)
            )>0;

            if (phoneExists){
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "PHONE_EXISTS",
                        "手机号已被使用"
                );
            }
        }

        //3.如果写了邮箱，检查是否重复
        if(email != null){
            boolean emailExists = userMapper.selectCount(
                new LambdaQueryWrapper<USER>()
                        .eq(USER::getEmail,email)
            )>0;

            if(emailExists){
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "EMAIL_EXISTS",
                        "邮箱已被使用了"
                );
            }
        }

        //4.创建需要被保护的对象
        USER user = new USER();

        user.setUsername(username);
        user.setNickname(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setGender(0);

        //5.对密码进行哈希处理
        String passwordHash = passwordEncoder.encode(
            request.getPassword()
        );

        user.setPassword_hash(passwordHash);

        //6.保存到数据库
        userMapper.insert(user);
    }


}

//在一个典型的Spring Boot项目中，代码通常分为三个主要层次：
//Controller（控制层）：接收前端HTTP请求
//Service（业务层）：处理注册、登录等业务逻辑
//Mapper（数据访问层）：执行数据库查询和修改
//MySQL（数据库）：真正存储用户数据