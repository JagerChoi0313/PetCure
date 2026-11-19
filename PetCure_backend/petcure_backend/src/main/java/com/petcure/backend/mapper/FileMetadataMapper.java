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

    @Select("""
            SELECT *
            FROM `file`
            WHERE id = #{fileId}
                AND user_id = #{userId}
            FOR UPDATE
            """)
    FileMetadata selectOwnedFileForUpdate(
            @Param("fileId")Long fileId,
            @Param("userId")Long userId
    );

    //@Param("userId") Long userId为方法参数起名，SQL中的#{userId}引用这个参数，并通过参数绑定传给数据库
}
