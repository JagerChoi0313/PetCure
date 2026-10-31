package com.petcure.backend.common;//统一响应对象，描述“返回给前端的数据长什么样”
//HTTP状态码也会正确设置：参数错误返回400，账号重复返回409，服务器异常返回500
import lombok.Getter;

@Getter
public class ApiResponse<T> {   //T是泛型，表示data可以装不同类型的数据
    //`ApiResponse<String>`的数据是字符串，`ApiResponse<void>表示没有业务数据`
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code,String message,T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    //成功，并返回数据
    public static <T>ApiResponse<T> success(T data){
        return new ApiResponse<>("SUCCESS","操作成功",data);
    }

    //成功，无返回数据
    public static ApiResponse<Void> success(){
        return success(null);
    }

    //失败
    public static ApiResponse<Void> error(String code,String message){
        return new ApiResponse<>(code,message,null);
    }
}
//后续Controller通过suceess()或error()创建返回结果