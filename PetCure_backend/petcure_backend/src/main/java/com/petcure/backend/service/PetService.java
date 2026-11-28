package com.petcure.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petcure.backend.duo.pet.PetResponse;
import com.petcure.backend.entity.Pet;
import com.petcure.backend.entity.USER;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.mapper.PetMapper;
import com.petcure.backend.mapper.USERMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PetService {
    private final PetMapper petMapper;

    public PetService(
            PetMapper petMapper
    ) {
        this.petMapper = petMapper;
    }

    //获取宠物信息
    public List<PetResponse> getCurrentPetInfo(Long userId) {    //因为一个用户会拥有多个宠物.所以这里是一个集合，要用List


        if (userId == null) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "用户不存在请重新登录"
            );
        }
        //查出List<Pet>，返回类型是List<PetResponse>
        //这个userId之后由Controller层从登录凭证中获取

        LambdaQueryWrapper<Pet> query = new LambdaQueryWrapper<>();
        query.eq(Pet::getUser_id, userId)
                .eq(Pet::getStatus, 1)
                .orderByDesc(Pet::getCreated_at)
                .orderByDesc(Pet::getId);

        List<Pet> pets = petMapper.selectList(query);

        List<PetResponse> result = new ArrayList<>();


        //把查到的每只pet都转化为Response
        //遍历pets，每次取出一只Pet，用它的getter创建一个PetResponse，加入结果列表
        for (Pet pet : pets) {
            PetResponse response = new PetResponse(
                    pet.getId(),
                    pet.getName(),
                    pet.getSpecies(),
                    pet.getBreed(),
                    pet.getSex(),
                    pet.getBirthday(),
                    pet.getBirthday_estimated(),
                    pet.getAvatar_url(),
                    pet.getColor(),
                    pet.getNeutered_status()
            );
            result.add(response);
        }

        return result;
    }
}
