package com.petcure.backend.service;
//JwtService负责在用户名和密码验证成功后，生成JWT。密码验证仍然由后续的AuthService完成
//JwtConfig:准备JWT签发器和验证器
//SecurityConfig:规定哪些接口需要身份认证、并使用JWT验证器
//JwtService：调用签发器，给登录成功的用户生成JWT

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service    //这是一个业务服务类，把它创建为Bean，交给Spring容器管理
//以后在AuthService中，就能通过构造器注入使用
public class JwtService {
    private final JwtEncoder jwtEncoder;//JWT签发工具
    private final String issuer;//Token的签发者
    private final String audience;//Token的目标接收系统
    private final long accessTokenTtlSeconds;//Token有效期，单位为秒

    public JwtService( //初始化JWT服务
        JwtEncoder jwtEncoder,      //这个JwtEncorder正式前面JwtConfig中创建的Bean
        @Value("${app.jwt.issuer}")String issuer,   //读取yaml中的issue配置
        @Value("${app.jwt.audience}")String audience,//读取yaml中的audience配置
        @Value("${app.jwt.access-token-ttl-seconds}") //读取yaml中的ttl配置
        long accessTokenTtlSeconds
    ){
        if(accessTokenTtlSeconds<=0){   //检查有效期，如果配置为0或负数，就抛出异常。
            throw new IllegalArgumentException("JWT有效期必须大于0");//这样可以避免因为错误配置而签发没有合理有效期的Token
        }

        //把构造器参数赋值给类的成员变量
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    //最核心的方法：为已经通过身份验证的用户生成Jwt访问令牌
    public Jwt createAccessToken(Long userId,Long tokenVersion){
        if(userId == null || userId<=0){
            throw new IllegalArgumentException("用户ID必须为正数");//检查用户ID
        }
        //另外，检查用户ID合法不等于验证用户身份。
        // JwtService默认相信调用它的业务代码已经完成了登录认证，它本身不会检查密码

        if(tokenVersion == null || tokenVersion < 0){
            throw new IllegalArgumentException("登录凭证版本号不能为空或负数");
        }
        //计算Jwt过期时间
        Instant now = Instant.now();    //Instant是Java提供的时间类，表示时间线上的一个具体时刻
        Instant expiresAt = now.plusSeconds(accessTokenTtlSeconds);//表示现在的时间加上间隔时间

        //构造JWT Claims，Claims是JWT中Payload（载荷）部分的数据
        //它用于记录Token的各种声明，例如谁签发、属于哪个用户、什么时候过期
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)     //iss:签发者
                .audience(List.of(audience))    //aud：接收者
                .subject(userId.toString())     //sub：用户身份
                .issuedAt(now)                  //iat：签发时间
                .expiresAt(expiresAt)           //exp：过期时间
                .id(UUID.randomUUID().toString())   //jti：Token唯一标识。UUID是一种用于生成标识符的机制
                .claim("token_version",tokenVersion)
                .build();

        //这里创建的是JWT的头部
        //它主要告诉JWT编码器：这个Token要使用HS256算法进行签名
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(//真正生成Jwt
                JwtEncoderParameters.from(header,claims)//组合Header和Claims
        );
    }
}
