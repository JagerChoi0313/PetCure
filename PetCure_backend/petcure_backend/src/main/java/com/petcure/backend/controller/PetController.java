package com.petcure.backend.controller;

import com.petcure.backend.common.ApiResponse;
import com.petcure.backend.duo.pet.PetResponse;
import com.petcure.backend.service.PetService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService petService;

    public PetController(PetService petService){
        this.petService = petService;
    }

    @GetMapping("/mypets")
    public ApiResponse<List<PetResponse>>mypets(
            @AuthenticationPrincipal Jwt jwt
            ){
        Long userId = Long.valueOf(jwt.getSubject());   //取当前用户ID，让Spring把已认证的JWT传给这个方法。项目把用户ID放在JWT的subject里，`jwt.getSubject()` 取到字符串，再用 Long.valueOf(...) 转成 Service 需要的 Long。

        //查这个用户的宠物。调用已经写好的getCurrentPetInfo(userId)。按主人筛选、排除归档、排序、转成PetResponse
        List<PetResponse> pets = petService.getCurrentPetInfo(userId);

        return ApiResponse.success(pets);
    }
}
