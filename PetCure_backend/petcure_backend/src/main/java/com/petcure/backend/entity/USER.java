package com.petcure.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;//引入Lambok工具库，用于自动生成代码
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Getter     //Lambok注解，在编译时自动为类的所有属性生成
@Setter
@TableName("USER")//这个类对应数据中的USER表
public class USER {

    @TableId(value = "id",type = IdType.AUTO)//指定主键字段；AUTO对应数据库自增主键
    private Long id;//声明主键字段，类型为 Long（大包装类型，常对应数据库中的 BIGINT）

    //登录用户名
    private String username;

    //密码哈希
    private String password_hash;

    //昵称
    private String nickname;

    //头像标识
    private String avatar_url;

    //性别
    private Integer gender;

    //出生日期
    private LocalDate birthday;

    //职业
    private String occupation;

    //所在城市
    private String city;

    //个人简介
    private String bio;

    //手机号
    private String phone;

    //邮箱
    private String email;

    //最后登录时间
    private LocalDateTime last_login_at;

    //创建时间，由数据库维护
    @TableField(
            insertStrategy = FieldStrategy.NEVER,//在根据实体生成插入，更新SQL时，不主动写入该字段，让数据库维护这两个时间
            updateStrategy = FieldStrategy.NEVER
    )
    private LocalDateTime created_at;

    //最后更新时间，由数据库来维护
    @TableField(
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER
    )
    private LocalDateTime updated_at;

}
