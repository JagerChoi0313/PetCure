package com.petcure.backend.exception;//业务异常类，表示业务中明确发生的失败，例如用户名重复


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {//BusinessException 是一种 RuntimeException，也就是一种运行时异常。

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String code;

    public BusinessException(
            HttpStatus status,
            String code,
            String message
    ){
        super(message);
        this.status = status;
        this.code = code;
    }
}

//它表示业务代码能够明确判断的失败情况，例如“用户名已被使用”
