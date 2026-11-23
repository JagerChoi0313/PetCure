package com.petcure.backend.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.petcure.backend.duo.user.*;
import com.petcure.backend.entity.FileMetadata;
import com.petcure.backend.entity.USER;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.mapper.FileMetadataMapper;
import com.petcure.backend.mapper.USERMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class UserService {
    private final USERMapper userMapper;
    private final PasswordEncoder passwordEncoder;//注入密码处理器
    private final FileMetadataMapper fileMetadataMapper;
    private final FileStorageService fileStorageService;

    public UserService(
            USERMapper userMapper,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService,
            FileMetadataMapper fileMetadataMapper
    ){
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.fileMetadataMapper = fileMetadataMapper;
        this.fileStorageService = fileStorageService;
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

        //检查手机号是否被其它用户占用
        if(phone != null){
            boolean phoneExists = userMapper.selectCount(
                    new LambdaQueryWrapper<USER>()
                            .eq(USER::getPhone,phone)
                            .ne(USER::getId,userId)
            )>0;

            if (phoneExists){
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "PHONE_EXISTS",
                        "手机号已被使用"
                );
            }
        }

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


    //添加修改密码的方法
    @Transactional
    public void updateCurrentUserPassword(
        Long userId,
        Long tokenVersion,
        UpdateUserPasswordRequest request
    ){
        //1.查询用户，并确认请求使用的登录版本仍然有效
        USER user = userMapper.selectById(userId);

        if (user == null || tokenVersion == null || !tokenVersion.equals(user.getToken_version())) {
                    throw new BusinessException(
                            HttpStatus.UNAUTHORIZED,
                            "UNAUTHORIZED",
                            "登录状态已失效，请重新登录"
                    );
        }

        //2.核对当前密码
        if(!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword_hash()
        )){
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CURRENT_PASSWORD_INCORRECT",
                    "当前密码不正确"
            );
        }

        //新密码不能与原密码相同
        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword_hash()
        )){
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "NEW_PASSWORD_SAME_AS_OLD",
                    "新密码不能与当前密码相同"
            );
        }

        //4，生成新密码的哈希
        String newPasswordHash = passwordEncoder.encode(
                request.getNewPassword()
        );

        //5.同时更新密码哈希和登录凭证版本号
        int updateRows = userMapper.update(
                null,
                new LambdaUpdateWrapper<USER>()
                        .eq(USER::getId,userId)
                        .eq(USER::getToken_version,tokenVersion)
                        .set(USER::getPassword_hash,newPasswordHash)
                        .setSql("token_version=token_version+1")
        );

        //6.必须成功更新一条记录
        if (updateRows != 1){
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "登录状态已发生改变，请重新登录后重试"
            );
        }
    }

    @Transactional
    public UserProfileResponse updateCurrentUserAvatar(
            Long userId,
            UpdateUserAvatarRequest request
    ){
            //1.锁定当前用户
            USER user = userMapper.selectByIdForUpdate(userId);

            if(user == null){
                throw new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "用户不存在，请重新登录"
                );
            }

            //2.查询并锁定属于当前用户的文件
        FileMetadata metadata =
                fileMetadataMapper.selectOwnedFileForUpdate(
                        request.getFileId(),
                        userId
                );

          if (metadata == null){
              throw avatarFileUnavailable();
          }

          //3.临时文件和已关联文件允许使用，待清理文件不允许使用
        boolean usableStatus =
                Objects.equals(metadata.getStatus(),0)
                        || Objects.equals(metadata.getStatus(),1);

          //4.检查记录中的图片类型
        boolean supportedType =
                "image/jpeg".equals(metadata.getMime_type())
                        || "image/png".equals(metadata.getMime_type())
                        || "image/webp".equals(metadata.getMime_type());

        if(!usableStatus
                || !supportedType
                || !fileStorageService.isAvatarFileAvailable(
                metadata.getStorage_key()
        )){
            throw avatarFileUnavailable();
        }

        //5.临时文件改为已关联
        if (Objects.equals(metadata.getStatus(),0)){
            int fileRows = fileMetadataMapper.update(
                    null,
                    new LambdaUpdateWrapper<FileMetadata>()
                            .eq(FileMetadata::getId,metadata.getId())
                            .eq(FileMetadata::getUser_id,userId)
                            .eq(FileMetadata::getStatus,0)
                            .set(FileMetadata::getStatus,1)
            );

            if (fileRows!=1){
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "AVATAR_UPDATE_CONFLICT",
                        "文件状态发生变化，请重新选择头像"
                );
            }
        }

        //6.生成后端受控访问的路径
        String avatarUrl = "/api/files/"+metadata.getId()+"/content";

        //重复保存同一头像时，不必再次更新用户记录
        if (!Objects.equals(user.getAvatar_url(),avatarUrl)){
            int userRows = userMapper.update(
                    null,
                    new LambdaUpdateWrapper<USER>()
                            .eq(USER::getId,userId)
                            .set(USER::getAvatar_url,avatarUrl)
            );

            if (userRows != 1){
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "AVATAR_UPDATE_CONFLICT",
                        "头像保存失败，请重试"
                );
            }
        }

        //7,返回后更新个人资料
        return getCurrentUser(userId);
    }

    private BusinessException avatarFileUnavailable(){
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "AVATAR_FILE_UNAVAILABLE",
                "头像文件不可用，请重新上传"
        );
    }


    //添加头像地址提取文件ID的方法1

    //例如：/api/files/content -> 1
    //没有旧头像、路径格式不符合规定或编号无效时，返回null，跳过旧文件处理
    private Long extractAvatarFileId(String avatarUrl){
        String prefix = "/api/files/";
        String suffix = "/content";

        if (avatarUrl == null
                || !avatarUrl.startsWith(prefix)
                || !avatarUrl.endsWith(suffix)
                || avatarUrl.length() <= prefix.length() + suffix.length()
        ){
            return null;
        }

        String idText = avatarUrl.substring(
                prefix.length(),
                avatarUrl.length() - suffix.length()
        );

        try{
            Long fileId = Long.valueOf(idText);
            return fileId > 0? fileId:null;
        }catch (NumberFormatException exception){
            return null;
        }
    }

    //添加旧文件的标记方法
    private void markOldAvatarForCleanup(
            Long userId,
            String oldAvatarUrl,
            Long newFileId
    ){
        Long oldFileId = extractAvatarFileId(oldAvatarUrl);

        //没有旧头像，或重复保存同一头像，无需处理
        if (oldFileId == null || oldFileId.equals(newFileId)){
            return;
        }

        //锁定属于当前用户的旧文件
        FileMetadata oldFile =
                fileMetadataMapper.selectOwnedFileForUpdate(
                        oldFileId,
                        userId
                );

        if (oldFile == null
                || !Objects.equals(oldFile.getStatus(),1)
                || oldFile.getStorage_key() == null
                || !oldFile.getStorage_key().startsWith("avatars/")){

                return;
        }

        //仍然存在用户头像引用时，保留文件
        if (fileMetadataMapper.counterUserAvatarReferences(oldFileId)>0){
            return;
        }

        //没有引用，标记为清理
        int updatedRows = fileMetadataMapper.update(
                null,
                new LambdaUpdateWrapper<FileMetadata>()
                        .eq(FileMetadata::getId,oldFileId)
                        .eq(FileMetadata::getUser_id,userId)
                        .eq(FileMetadata::getStatus,1)
                        .eq(FileMetadata::getStatus,2)
        );

        if (updatedRows!=1){
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "AVATAR_UPDATE_CONFLICT",
                    "旧头像状态发生变化，请重试"
            );//这里继续使用已有的锁定查询，确保判断和修改旧文件期间，它不会被另一个1遵循相同锁定规则的操作同时修改
        }
    }

}
