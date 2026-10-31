package com.petcure.backend.exception;//全局异常处理器，接住异常，把错误整理成统一响应
//在没有全局异常处理时，Controller抛出异常会直接返回Spring默认的白页(Whitelabel Error Page)
//或未经封装的500堆栈错误，既不安全也不方便前端解析。该类通过拦截控制器抛出的各类异常，将其统一包装成标准响应格式ApiResponse<Void>,并匹配对应的HTTP状态码


import com.petcure.backend.common.ApiResponse;  //统一响应对象，描述“返回给前端的数据长什么样”
import lombok.extern.slf4j.Slf4j;       //提供日志注解，自动在类中生成log对象
import org.springframework.dao.DuplicateKeyException;       //Spring封装的 数据库唯一约束冲突异常
import org.springframework.http.HttpStatus;                 //HTTP状态码枚举类(如400，404，409，500等)
import org.springframework.http.ResponseEntity;             // Spring 用于封装 HTTP 响应（包括状态码、头信息、响应体）的容器
import org.springframework.http.converter.HttpMessageNotReadableException;       //请求体解析失败异常(JSON语法错误、类型不匹配、RequestBody为空)
import org.springframework.web.ErrorResponse;               //统一错误响应接口
import org.springframework.web.bind.MethodArgumentNotValidException;     //校验DTO参数失败时抛出的异常
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j//lombok提供的日志注解，自动在类中生成log对象
@RestControllerAdvice
public class GlobalExceptionHandler {



    //处理业务代码主动抛出的异常(如：余额不足，用户不存在等)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>>handleBusinessExcsption(
            //handleBusinessException是一个公共处理方法，用于接管业务异常，并向客户端返回一个状态码+规范化空数据响应体
            //ResponseEntity<>是Spring的HTTP响应包装器，代表整个HTTP响应报文
            //ApiResponse：项目中自定义的统一统一返回格式封装类（通常包含 code、message、data 等字段），是以json格式返回的
            BusinessException ex
    ){
        // 从自定义异常中动态获取 HTTP 状态码（ex.getStatus()）和错误码、提示信息
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(),ex.getMessage()));
    }



    /**
     * 2. 处理实体类参数校验失败异常（@Valid / @Validated 触发）
     * 例如：@NotBlank(message = "用户名不能为空") 触发时拦截
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>>handleValidationException(
            MethodArgumentNotValidException ex
    ){
        //从校验结果BingdingResult中提取所有的字段校验失败的提示信息
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error->error.getDefaultMessage())// 获取在 DTO 注解上定义的 message 属性值
                .filter(Objects::nonNull)// 过滤掉可能存在的 null 值
                .distinct()// 去重（防止同一个错误信息重复多次出现）
                .sorted()// 字典排序，确保同一组错误返回顺序一致
                .collect(Collectors.joining(";"));// 将多个错误提示使用中文分号“；”连接成一句完整描述

        // 兜底防护：如果注解未配置 message 或由于某些原因提示为空，赋予默认文案
        if (message.isBlank()){
            message = "请求参数不符合要求";
        }

        // 返回 HTTP 400 Bad Request，并附带组装好的多条错误信息
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR",message));

    }

    /**
     * 3. 处理请求体（JSON）反序列化失败的异常
     * 常见场景：
     * - 请求方式为 POST/PUT 但未传 Body
     * - JSON 语法错误（如缺少逗号、引号未闭合）
     * - 字段类型转换失败（例如属性要求 Long，前端传了 "abc"）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>>handleUnreadableBody(
            HttpMessageNotReadableException ex
    ){
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                   "INVALID_REQUEST_BODY",
                   "请求体缺失或JSON格式不正确,请检查提交的数据"
                ));
    }



    /**
     * 4. 数据库唯一键约束冲突（兜底处理）
     * 场景：并发插入或未做前置判断导致的 DuplicateKey（如手机号重复、用户名重复注册）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>>handleDuplicateKeyException(
            DuplicateKeyException ex
    ){
        // 返回 HTTP 409 Conflict（资源冲突），避免将底层 SQL 异常（如 MySQL 1062）暴露给外部
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "DATA_CONFLICT",
                        "数据已存在，请检查后重新提交"
                ));
    }



    /**
     * 5. 最底层的通用异常兜底（捕获所有未被上述方法拦截的 Exception）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>>handleException(
            Exception ex
    ){
        //保留Spring MVC异常原本的状态码，例如404，405
        // 5.1 针对 Spring 内部的标准 Web 异常（如 404 NoHandlerFound、405 MethodNotAllowed 等）
        // Spring Boot 3+ 默认实现了 ErrorResponse 接口
        if(ex instanceof ErrorResponse errorResponse){
            if(errorResponse.getStatusCode().is5xxServerError()){
                log.error("请求处理失败",ex);
            }

            // 保留原本的状态码（如 404、405）与响应头，仅把响应体统一格式化为 ApiResponse
            return ResponseEntity
                    .status(errorResponse.getStatusCode())
                    .headers(errorResponse.getHeaders())
                    .body(ApiResponse.error(
                            "HTTP_ERROR",
                            "请求无法完成"
                    ));
        }



        // 5处理未预期的未知代码 Bug（空指针 NullPointerException、下标越界等）
        // 关键安全点：在服务器日志中打印完整堆栈用于排错，但绝对不把内部异常细节暴露给客户端

        //完整异常记录在后端日志中
        log.error("未处理的服务异常",ex);
        // 统一返回 HTTP 500 Internal Server Error，并给用户返回安全的通用提示文案
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                   "INTERNAL_ERROR",
                   "服务器暂时不可用，请稍后重试"
                ));
    }
}
