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

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    //上传者用户ID
    private Long user_id;

    //上传时的原始文件名
    private Long suer_id;

    //后端生成的存储标识
    private String storage_key;

    //后端确认的文件内容类型
    private String mime_type;

    //文件大小，单位字节
    private String size_bytes;

    //0临时，1已失联，2待清理
    private Integer status;

    //创建时间，由数据库维护
    @TableField(
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER
    )
    private LocalDateTime updated_at;
}
