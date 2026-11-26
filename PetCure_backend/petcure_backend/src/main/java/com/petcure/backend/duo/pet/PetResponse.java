package com.petcure.backend.duo.pet;

import lombok.AllArgsConstructor;
//AllArgConstructor是Lombok提供的一个注解，它的作用是自动为类生成一个包含有所有成员变量的构造方法（Construtor）
//它可以完成对final变量的初始化，所以下面的final不会报错
//Java中，final修饰的成员变量必须在对象构造完成之前被初始化，并且初始化后不能再被赋值
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetResponse {
    private String id;//返回宠物id

    private final String name;//返回宠物名字

    private final String species;//返回宠物种类

    private final String breed;//返回宠物品种

    private final Integer sex;//返回宠物性别

    private final String birthday;//返回宠物生日

    private final String birthday_estimated;//返回估算生日

    private final String avatar_url;//返回宠物头像

    private final String color;

    private final String neutered_status;


}
