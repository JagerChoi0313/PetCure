package com.petcure.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcure.backend.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


//这个Mapper操作的是FileMetadata对应的表
//虽然接口内部没有手写方法，但已经继承了常用操作：insert(...),selectById(...),selectCount(...)
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
    //给文件Mapper添加归属检查和锁定查询

    //Select声明查询SQL，MyBatis负责执行，并把结果映射成USER或FileMetadata对象。这两个方法都不需要另写XML
    //后面会在Service层中依次锁定用户，锁定文件，再更新用户头像和文件状态.事物提交或回滚后，锁会释放
    @Select("""
            SELECT *
            FROM `file`
            WHERE id = #{fileId}
                AND user_id = #{userId}
            FOR UPDATE
            """)    //含义是查找这个文件而且它必须属于当前登录用户

    FileMetadata selectOwnedFileForUpdate(
            @Param("fileId")Long fileId,
            @Param("userId")Long userId
    );
    //添加归属检查和锁定查询
    //文件存在，且属于当前用户，返回文件记录
    //文件不存在，返回null
    //文件属于其它用户，返回null
    //后面的service可以统一提示“文件头像不可用”,不向用户区分文件不存在还是属于别人
    //文件记录也要锁定，因为关联状态即将从“临时”变成“已关联”，这个过程要与后续稳健清理逻辑协调

    @Select("""
            SELECT f.*
            FROM `file` f
            INNER JOIN `USER` u ON u.id = f.user_id
            WHERE f.id = #{fileId}
            AND f.user_id = #{userId}
            AND f.status = 1
            AND u.avatar_url = CONCAT('/api/files/',f.id,'/content') 
            """)
    FileMetadata selectCurrentAvatarForRead(
            @Param("fileId")Long fileId,
            @Param("userId")Long userId
    );

    //它负责查询“属于当前用户，而且仍然被用作当前头像”的文件
}
