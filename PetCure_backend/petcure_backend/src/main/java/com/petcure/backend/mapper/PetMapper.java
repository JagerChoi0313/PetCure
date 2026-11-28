package com.petcure.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcure.backend.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PetMapper extends BaseMapper<Pet> {
//    @Select("""
//            SELECT *
//            FROM `Pet`
//            WHERE id = #{petId}
//            FOR UPDATE
//            """)
    //我们现在已经声明了Pet selectList(Long petId)。它的SQL按宠物ID查一条记录，还用了FOR UPDATE
    //BaseMapper<Pet>已经提供了接收查询条件并返回List<Pet>的selectList方法
    //因此只需保留@Mapper和继承BaseMapper<Pet>的接口即可。两个方法同名也容易造成MyBatis冲突
//    Pet selectList(@Param("petId")Long petId);
//
}
