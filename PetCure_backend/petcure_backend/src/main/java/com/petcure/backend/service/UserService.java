package com.petcure.backend.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.petcure.backend.duo.user.UpdateUserContactRequest;
import com.petcure.backend.duo.user.UpdateUserProfileRequest;
import com.petcure.backend.duo.user.UserProfileResponse;
import com.petcure.backend.entity.USER;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.mapper.USERMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Service
public class UserService {
    private final USERMapper userMapper;
    private final PasswordEncoder passwordEncoder;//注入密码处理器

    public UserService(
            USERMapper userMapper,
            PasswordEncoder passwordEncoder
    ){
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse getCurrentUser(Long userId){
        USER user = userMapper.selectById(userId);

        if(user == null){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "用户不存在，请重新登录"
            );
        }

        //1.获取生日
        LocalDate birthday = user.getBirthday();

        //2.根据上海时区的当前日期计算周岁、
        Integer age = null;

        if(birthday != null){
            LocalDate today = LocalDate.now(
                    ZoneId.of("Asia/Shanghai")
            );

            age = Period.between(birthday,today).getYears();
        }

        //3.将数据库中的资料组织成接口返回对象
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar_url(),
                user.getGender(),
                birthday,
                age,
                user.getOccupation(),
                user.getCity(),
                user.getBio(),
                user.getPhone(),
                user.getEmail(),
                user.getCreated_at(),
                user.getLast_login_at()
        );
    }

    //本次新增保存逻辑
    @Transactional
    public UserProfileResponse updateCurrentUser(
        Long userId,
        UpdateUserProfileRequest request
    ){
        //1.根据当前登录用户ID，只更新允许修改的资料字段
        int updateedRows = userMapper.update(
                null,
                new LambdaUpdateWrapper<USER>()
                        .eq(USER::getId,userId)
                        //“set”确定修改哪些字段，这里的set也可以清空旧值
                        .set(USER::getNickname,request.getNickname())
                        .set(USER::getGender,request.getGender())
                        .set(USER::getBirthday,request.getBirthday())
                        .set(USER::getOccupation,request.getOccupation())
                        .set(USER::getCity,request.getCity())
                        .set(USER::getBio,request.getBio())
        );

        //2.更新行数为0时，确认用户是否仍然存在
        //某些数据库连接配置下，提交相同资料也可能返回0
        if (updateedRows == 0 && userMapper.selectById(userId)==null){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "用户不存在，请重新登录"
            );
        }

        //重新查询
        return getCurrentUser(userId);
    }

    @Transactional
    public UserProfileResponse updateCurrentUserContact(
            Long userId,
            UpdateUserContactRequest request
    ){
        //1.查询当前用户
        USER user = userMapper.selectById(userId);

        if (user==null){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "用户不存在，请重新登录"
            );
        }

        //2.核对当前密码
        boolean passwordCorrect = passwordEncoder.matches(//密码核对使用matches
                request.getCurrentPassword(),//这个参数是用户提交的原始密码
                user.getPassword_hash()//这个是数据库里的密码哈希
        );
        if(!passwordCorrect){
            throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "CURRENT_PASSWORD_INCORRECT",
              "当前密码不正确"
            );
        }

        String phone = request.getPhone();
        String email = request.getEmail();

        //检查邮箱是否被其它用户使用
        if (email!=null){
            boolean emailExists = userMapper.selectCount(//查重时要排除自己
                    new LambdaQueryWrapper<USER>()
                            .eq(USER::getEmail,email)//eq表示相等，ne表示不相等
                            .ne(USER::getId,userId)
                    )>0;

                    if (emailExists){
                        throw new BusinessException(
                                HttpStatus.CONFLICT,
                                "EMAIL_EXISTS",
                                "邮箱已被使用"
                        );
                    }
                }

        //5,保存整组联系方式，允许其中的一项为null
        int updatedRows = userMapper.update(
                null,
                new LambdaUpdateWrapper<USER>()
                        .eq(USER::getId, userId)
                        .set(USER::getPhone,phone)
                        .set(USER::getEmail, email)
        );

        //相同值重复保存可能返回0，需要区分用户不存在的情况
        if(updatedRows == 0 && userMapper.selectById(userId) == null){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "用户不存在，请重新登录"
            );
        }
        //返回保存后的完整资料
        return getCurrentUser(userId);
    }
}
