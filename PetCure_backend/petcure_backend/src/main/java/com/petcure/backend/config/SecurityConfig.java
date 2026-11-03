package com.petcure.backend.config;
//核心安全配置类，负责决定：
//哪些接口不需要登录就能访问？
//哪些接口必须携带 JWT？
//收到 JWT 后，如何调用 JwtDecoder 验证？
//没登录时为什么返回 401？
//已登录但权限不足时为什么返回 403？

//JwtConfig定义怎么验证token，SecurityConfig定义什么时候需要验证token，以及验证失败后该怎么处理

import com.petcure.backend.common.ApiResponse;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.security.Security;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(//安全过滤链// 可以把它想象成PetCure后端接口前面的安全检查站
    //用户请求通常先经过Spring Secuirty的过滤器，再到Controller
    //Spring Security 的过滤器链不仅能处理 JWT，还能实现登录、授权、异常处理等功能
            HttpSecurity http,  //HttpSecurity 可以理解成一个安全规则配置工具。
            JwtDecoder jwtDecoder,  //之前 JwtConfig 创建的 Bean，用来验证 JWT。
            ObjectMapper objectMapper  //是 JSON 序列化工具，把 Java 对象转换成 JSON，主要用于统一错误响应。
    )throws Exception{

        //未登录。Token无效或过期时，返回401
        AuthenticationEntryPoint authenticationEntryPoint=
                //它主要处理：用户没有有效的认证身份，却访问了需要认证的资源。
                (request,response,exception)->{
                    response.setHeader(
                            HttpHeaders.WWW_AUTHENTICATE,
                            "Bearer"
                    );

                    writeErrorResponse(
                            response,
                            objectMapper,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "UNAUTHORIZED",
                            "未登录或登录已失效，请重新登录"
                    );
                };

        //已登录但权限不足时，返回403
        //用户已经认证成功，但是不具备访问资源的权限。
        AccessDeniedHandler accessDeniedHandler =
                (request,response,exception)->{
                    writeErrorResponse(
                        response,
                        objectMapper,
                        HttpServletResponse.SC_FORBIDDEN,
                        "FORBIDDEN",
                        "没有权限访问资源"
                    );
                };

        http    //配置HTTP安全策略
            //本项目通过Authorization请求头携带JWT认证
                .csrf(AbstractHttpConfigurer::disable)
            //CSRF是跨站请求伪造攻击。传统网站如果使用浏览器自动携带的Session Cookie来认证，通常需要考虑CSRF防护
            //如果认证凭证只通过客户端显式添加的 Authorization 请求头传递，而不依赖浏览器自动携带的 Cookie，那么通常可以关闭 CSRF 防护


            //不使用Session保存登录身份
                .sessionManagement(session ->session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)//STATELESS 的意思是无状态。
                )

                //关闭默认登录方式和默认退出接口
                .formLogin(AbstractHttpConfigurer::disable)     //不使用 Spring Security 默认表单登录
                .httpBasic(AbstractHttpConfigurer::disable)     //不使用 HTTP Basic 认证
                .logout(AbstractHttpConfigurer::disable)        //不使用 Spring Security 默认退出登录处理

                //配置接口访问规则
                .authorizeHttpRequests(authorize -> authorize
                        //允许框架内部的错误分派，保留原始错误响应
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        //注册、登录接口允许直接访问
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        //其他请求必须通过身份认证
                        .anyRequest().authenticated()
                )

                //使用JwtConfig中的校验器验证JWT
                .oauth2ResourceServer(resourceServer->resourceServer

                    .jwt(jwt->jwt.decoder(jwtDecoder))
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                )
                //统一处理接口访问被拒绝的情况
                .exceptionHandling(exception->exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    //待认证、权限错误转换成统一的JSON响应
    private void writeErrorResponse(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String code,
            String message
    )throws IOException{

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(code,message)

        );
    }
}
