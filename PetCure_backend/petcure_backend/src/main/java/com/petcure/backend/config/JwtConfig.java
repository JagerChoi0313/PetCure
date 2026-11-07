package com.petcure.backend.config;
//这段JwtConfig的作用是告诉Spring Security：使用什么密钥签发JWT，以及收到JWT后按照什么规则验证它

import com.nimbusds.jwt.JWTClaimNames;
import com.petcure.backend.mapper.JwtRevocationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

//Configuration这个注解告诉Spring：这是一个配置类，里面可能定义了一些需要交给Spring容器管理的对象
//SpringBoot启动时，会扫描这个类，然后处理里面的@Bean方法
@Configuration
public class JwtConfig {

    //将配置中的Base64文本还原成签名密钥
    //将这个方法返回的SecretKey对象注册到Spring容器，这样其他类可以通过依赖注入使用它，而不需要每次手动创建
    //在这个类中我定义了三个Bean：SecretKey（提供JWT签名和验证所需的密钥），
    // JwtEncorder（负责生成签名的JWT），
    // JWTDecorder（负责解析、验证JWT）
    @Bean
    public SecretKey jwtSecretKey(//第一个Bean：创建JWT密钥
            @org.springframework.beans.factory.annotation.Value("${app.jwt.secret-base64}") String secretBase64  //读取配置
            //Spring会读取app.jwt.secret-base64的值，并传入secretBase64参数。这里使用环境变量来提供真实密钥
            //为什么密钥是Base64格式？因为JWT签名使用的是二进制密钥，但配置文件通常存储文本。因此我们可以把二进制密钥编码成Base64文本，方便配置和读取
    ){
        //把配置中的Base64字符串还原成原始的密钥字节
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);

        //检查密钥长度
        if(keyBytes.length<32){//为什么要求至少32字节，因为后面使用的是MacAlgorithm.HS256，要求至少256位强度的密钥
            throw new IllegalArgumentException(
                    "JWT签名密钥经过Base64解码后不能少于32字节"
            );
        }
        return new SecretKeySpec(keyBytes,"HmacSHA256");//构造SecretkeySpec可以理解为一个密钥对象的实现
    }                                                            //主要接受两个参数 ：keybytes：真正的密钥数据，HmacSHA256：这个密钥对应的算法名称

    //配置Token签发器
    @Bean//为什么我们没有主动调用这个方法，也没有传参数，Spring知道jwtSecretKey是什么？
    //因为前面已经注册过了public SecretKey jwtSecretKey(...)，容器中已经有一个SecretKey对象
    //当Spring创建JwtEncorder时，它会自动注入这个对象，这就是Spring的依赖注入
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey){
        return NimbusJwtEncoder     //这是Spring Security提供的JWT编码器实现。负责根据JWT的Header和Claims信息生成带签名的Token
                .withSecretKey(jwtSecretKey)//对创建的密钥进行签名
                .algorithm(MacAlgorithm.HS256)//指定算法位HS256
                .build();//构建并返回真正的NimbusJwtEncorder对象
    }

    //配置Token校验器
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @org.springframework.beans.factory.annotation.Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience,
            JwtRevocationMapper jwtRevocationMapper
    ){
        //构建解码器，这一步告诉Spring Security，使用HS256算法和同一个密钥来验证Token的签名
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        //默认有效期检查，并检查签发者（默认校验器）
        //输入一个JWT，检查它是否满足规则，并返回校验成功或失败的结果
        OAuth2TokenValidator<Jwt>defaultValidator =
                JwtValidators.createDefaultWithIssuer(issuer);

        //检查Token是否用于本系统API（Audience校验器）
        //这是检查：这个JWT是不是签发给PetCure API的
        OAuth2TokenValidator<Jwt>audienceValidator=
                new JwtAudienceValidator(audience);

        //要求Token必须包含过期时间（时间校验器）
        OAuth2TokenValidator<Jwt> expirationRequired=
                new JwtClaimValidator<Instant>(
                        JwtClaimNames.EXP,//它代表 JWT 的 exp 字段，也就是过期时间
                        Objects::nonNull  //检查 JWT 的 exp 字段是否存在且不为 null。
                );

        OAuth2TokenValidator<Jwt>existingValidators =
                new DelegatingOAuth2TokenValidator<>(

                            defaultValidator,
                            audienceValidator,
                            expirationRequired
                );

        decoder.setJwtValidator(jwt->{
            OAuth2TokenValidatorResult result = existingValidators.validate(jwt);
            if(result.hasErrors()){
                return result;
            }

            String jti = jwt.getId();
            if (jti==null || jti.isBlank()){
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token","JWT缺少令牌编号",null)
                );
            }

            String tokenIssuer = jwt.getClaims().get(JwtClaimNames.ISS).toString();
            if (jwtRevocationMapper.countRevocation(tokenIssuer,jti)>0){
                return OAuth2TokenValidatorResult.failure(
                  new OAuth2Error("Invalid_token","JWT已撤销",null)
                );
            }

            return OAuth2TokenValidatorResult.success();
        });


        return decoder;
    }
}
