package com.petcure.backend.entity;
import com.baomidou.mybatisplus.annotation.IdType;//引入MyBatis-Plus框架注解,用于数据表和主键的映射
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;//引入Lambok工具库，用于自动生成代码
import lombok.Setter;


@Getter     //Lambok注解，在编译时自动为类的所有属性生成
@Setter
@TableName("USER")//指定实体对应的数据表
public class USER {

    @TableId(value = "id",type = IdType.AUTO)//指定主键字段；AUTO对应数据库自增主键
    private Long id;//声明主键字段，类型为 Long（大包装类型，常对应数据库中的 BIGINT）
}
