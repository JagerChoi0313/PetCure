package com.petcure.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcure.backend.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;


//这个Mapper操作的是FileMetadata对应的表
//虽然接口内部没有手写方法，但已经继承了常用操作：insert(...),selectById(...),selectCount(...)
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
