package com.petcure.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcure.backend.entity.USER;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface USERMapper extends BaseMapper<USER> {

    //给用户Mapper添加锁定查询
    @Select("""
            SELECT *
            FROM `USER`
            WHERE id = #{userId}
            FOR UPDATE
            """)
    USER selectByIdForUpdate(@Param("userId")Long userId);

}

//BaseMapper<User>已经提供了常用的数据库操作，例如:
//userMapper.insert(user);       // 新增用户
//userMapper.selectById(id);     // 根据ID查询
//userMapper.updateById(user);   // 根据ID更新
//目前这个接口保持空的就可以，不需要另外编写 XML。