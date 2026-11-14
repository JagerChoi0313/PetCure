package com.petcure.backend.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("file")
public class FileMetadata {

    //表示新增时由MySQL生成文件ID，后续调用fileMetadataMapper.insert(fileMetadata)成功后
    // ，MyBatis-Plus 会将生成的ID回填到这个对象中，可以通过fileMetadata.getId()取得它，再返回给前端
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    //上传者用户ID
    private Long user_id;

    //上传时的原始文件名
    private String original_name;

    //后端生成的存储标识
    private String storage_key;

    //后端确认的文件内容类型
    private String mime_type;

    //文件大小，单位字节
    private Long size_bytes;

    //0临时，1已关联，2待清理
    private Integer status;

    //创建时间，由数据库维护
    @TableField(
            //表示 MyBatis-Plus 根据实体生成插入、更新 SQL 时，不主动写入这些字段，交给数据库的默认值和自动更新规则维护
            //数据库生成的时间不会因此自动回填到刚插入的 Java 对象中；如果需要拿到这些时间，应当重新查询记录。
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER
    )
    private LocalDateTime created_at;

    //最后更新时间，由数据库维护
    @TableField(
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER
    )
    private LocalDateTime updated_at;
}
