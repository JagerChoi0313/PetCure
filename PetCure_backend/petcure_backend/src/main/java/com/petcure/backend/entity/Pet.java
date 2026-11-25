package com.petcure.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
public class Pet {
        @TableId(value="id",type= IdType.AUTO)
        private Long id;//宠物ID

        private Long user_id;//宠物主人ID

        private String name;//宠物名字

        private String species;//宠物物种，如猫和狗

        private String breed;//宠物品种

        private Integer sex;//宠物性别

        private LocalDate birthday;//宠物生日

        private Integer birthday_estimated;//宠物生日是否为估算，0否，1是

        private String avatar_url;//宠物头像地址和存储标识

        private String color;//宠物毛发颜色

        private Integer neutered_status;//宠物绝育状态，0未知，1未绝育，2已绝育

        private LocalDate arrival_date;//宠物到家日期

        private String remark;//宠物备注

        private Integer status;//宠物档案状态

        //创建时间，由数据库维护
        @TableField(
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER
        )
        private LocalDateTime created_at;

        @TableField(
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER
        )
        private LocalDateTime updated_at;
}
