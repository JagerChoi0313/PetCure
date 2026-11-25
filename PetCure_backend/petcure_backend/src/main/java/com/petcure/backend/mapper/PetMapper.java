package com.petcure.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcure.backend.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PetMapper extends BaseMapper<Pet> {
    @Select("""
            SELECT * 
            FROM `Pet`
            WHERE id = #{petId}
            FOR UPDATE
            """)
    Pet selectList(@Param("petId")Long petId);

}
