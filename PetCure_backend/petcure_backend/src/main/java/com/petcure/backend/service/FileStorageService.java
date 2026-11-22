package com.petcure.backend.service;

import com.petcure.backend.config.FileStorageProperties;
import com.petcure.backend.duo.file.FileContent;
import com.petcure.backend.duo.file.FileUploadResponse;
import com.petcure.backend.entity.FileMetadata;
import com.petcure.backend.exception.BusinessException;
import com.petcure.backend.mapper.FileMetadataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
public class FileStorageService {

    //定义头像大小上限
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;//头像最大为5MB，单位为字节

    //读取头像时，最多允许2000万像素
    private static final long MAX_AVATAR_PIXELS = 20_000_000L;
    //文件实际保存的根目录
    private final Path rootDir;

    //注入文件Mapper
    private final FileMetadataMapper fileMetadataMapper;



    public FileStorageService(
            FileStorageProperties fileStorageProperties,
            FileMetadataMapper fileMetadataMapper
    ){
        this.fileMetadataMapper = fileMetadataMapper;

        //将配置中的字符串转换为路径对象
        Path configuredDir = Path.of(//"Path.of"将路径字符串转换为Java路径对象
                fileStorageProperties.getRootDir()
        ).normalize();      //整理路径中的“.”、“..”等部分

        if(!configuredDir.isAbsolute()){    //判断路径是否为绝对路径
            throw new IllegalArgumentException(
                    "文件存储根目录必须使用绝对路径"
            );
        }

        try{
            //目录不存在时，连同父目录一起创建
            Files.createDirectories(configuredDir);//创建目录及缺失的父目录；目录已存在时可以继续使用

            //获取实际存在的目录路径，供后续保存文件使用
            this.rootDir = configuredDir.toRealPath();  //获取实际存在的路径，并解析符号链接等情况
        }catch(IOException exception){
            throw new IllegalArgumentException(
                    "无法准备文件存储目录，请检查路径和访问权限",
                    exception
            );
        }
    }

    private void validateAvatarFile(MultipartFile file){
        //1.必须上传有内容的文件
        if (file == null || file.isEmpty()){
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_EMPTY",
                    "请选择非空的头像文件"
            );
        }

        //2.文件大小不能超过5MB
        if (file.getSize()>MAX_AVATAR_SIZE){
            throw new BusinessException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "FILE_TOO_LARGE",
                    "头像文件不能超过5MB"
            );
        }
    }
    //为什么配置限制之外，还要在 Service 检查
    //两者职责不同：1.spring.servlet.multipart在请求解析阶段限制上传大小    2.Service明确执行“头像不能超过5MB”的业务规则
    //正常 HTTP 上传超限时，通常会先被框架拦截，因此不一定会执行到这里的大小检查。后续我们会补充框架上传异常的统一提示。

    //创建图片上传发生异常的方法
    private BusinessException invalidAvatarImage(String message) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_AVATAR_IMAGE",
                message
        );
    }

    //添加图片内容校验方法
    private String validateAvatarImageContent(MultipartFile file){
        //先执行已有的空文件和大小检查
        validateAvatarFile(file);

        try(
                InputStream input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input)
                ){
            if (imageInput == null){
                throw invalidAvatarImage("无法获取头像图片");
            }
             //根据文件内容寻找能够读取它的图片读取器
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()){
                throw invalidAvatarImage("文件不是可识别图片");
            }

            ImageReader reader = readers.next();

            try{
                reader.setInput(imageInput);

                //获取实际识别出的格式
                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                if("jpg".equals(format)){
                    format = "jpeg";
                }

                if (!"jpeg".equals(format)
                        && !"png".equals(format)
                        && !"webp".equals(format)){
                    throw invalidAvatarImage(
                            "头像只支持JPG、PNG、WebP图片"
                    );
                }
                //完整读取之前，先检查宽高及像素数量
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width<=0||height<=0){
                    throw invalidAvatarImage("图片尺寸无效");
                }

                if((long)width * height > MAX_AVATAR_PIXELS){
                    throw invalidAvatarImage(
                      "头像图片不能超过2000万像素"
                    );
                }

                //尝试解码第一张图像，检查是否能正常读取
                BufferedImage image = reader.read(0);

                if (image == null){
                    throw invalidAvatarImage("图片内容无法正常读取");
                }

                //不再使用解码后的图像，释放其可释放的资源
                image.flush();
                return format;

            }finally {
                reader.dispose();
            }
        }catch(IOException|IllegalArgumentException exception){
            throw invalidAvatarImage(
                    "图片读取失败，请使用有效的JPG、PNG和WebP图片"
            );
        }
    }

    //添加保存图片的方法
    private String saveAvatarFile(MultipartFile file,String format){
        //format必须来自前面的图片内容校验结果
        String extension = switch (format){
            case "jpeg"->"jpg";
            case "png"->"png";
            case "webp"->"webp";
            default -> throw invalidAvatarImage("不支持的头像图片格式");
        };

        //使用后端生成的随机名称，不能用原文件名拼接路径
        String filename = UUID.randomUUID() + "." + extension;
        String storageKey = "avatars/" + filename;

        Path target = rootDir.resolve(storageKey);
        boolean fileCreated = false;

        try{
            //准备图片所在的父目录
            Files.createDirectories(target.getParent());

            try(InputStream input = file.getInputStream()){
                //文件必须是新建的，不能覆盖已有文件
                try(OutputStream output=Files.newOutputStream(
                        target,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                )){
                    fileCreated = true;

                    //将上传的文件内容写入磁盘
                    input.transferTo(output);
                }
            }
            return storageKey;
        }catch (IOException exception){
            //如果已经创建文件，写入失败时尝试清理残留
            if (fileCreated){
                try{
                    Files.deleteIfExists(target);
                }catch(IOException cleanupException){
                    exception.addSuppressed(cleanupException);
                }
            }

            log.error("保存头像文件失败",exception);

            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_SAVE_FAILED",
                    "头像保存失败，请稍后重试"
            );
        }
    }

    //添加上传业务方法
    @Transactional
    public FileUploadResponse uploadAvatar(
            Long userId,
            MultipartFile file
    ){
        //1，校验文件，取得实际图片格式
        String format = validateAvatarImageContent(file);

        //2.根据实际格式确定内容类型
        String mimeType = switch(format){
            case "jpeg"->"image/jpeg";
            case "png" -> "image/png";
            case "webp"-> "image/webp";
            default -> throw invalidAvatarImage("不支持的头像图片格式");
        };

        //处理原文件名，只用于记录和展示
        String originalName = file.getOriginalFilename();

        if(originalName == null){
            originalName = "";
        }

        originalName = originalName.replace('\\','/');
        originalName = originalName.substring(
                originalName.lastIndexOf('/')+1
        ).strip();

        if(originalName.isEmpty()){
            originalName = "avatar";
        }

        if(originalName.length()>255 || originalName.chars().anyMatch(Character::isISOControl)){
            throw new BusinessException(
              HttpStatus.BAD_REQUEST,
              "INVALID_FILE_NAME",
              "文件名不能超过255个字符，且不能包含控制字符"
            );
        }

        //4.保存磁盘文件
        String storageKey = saveAvatarFile(file,format);

        //5.如果后续数据库事物回滚，清理刚保存的1文件
        registerRollbackCleanup(storageKey);

        //6.创建临时文件记录
        FileMetadata metadata = new FileMetadata();
        metadata.setUser_id(userId);
        metadata.setOriginal_name(originalName);
        metadata.setStorage_key(storageKey);
        metadata.setMime_type(mimeType);
        metadata.setSize_bytes(file.getSize());
        metadata.setStatus(0);

        int insertedRows = fileMetadataMapper.insert(metadata);

        if(insertedRows != 1 || metadata.getId() == null){
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_METADATA_SAVE_FAILED",
                    "文件信息保存失败，请稍后重试"
            );
        }

        //7.返回文件ID及必要描述信息
        return new FileUploadResponse(
                metadata.getId(),
                metadata.getMime_type(),
                metadata.getSize_bytes()
        );
    }

    private void registerRollbackCleanup(String storageKey){
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status){
                        if(status == TransactionSynchronization.STATUS_ROLLED_BACK){
                            try{
                                Files.deleteIfExists(
                                        rootDir.resolve(storageKey)
                                );
                            }catch (IOException exception){
                                log.error(
                                        "事物回滚后清理头像文件失败，stroageKey={}",
                                        storageKey,
                                        exception
                                );
                            }
                        }else if(status == TransactionSynchronization.STATUS_UNKNOWN){
                            log.error(
                                     "上传结果未知，保留文件待核对，storageKey={}",
                                    storageKey
                            );
                        }
                    }
                }
        );
    }

    //添加磁盘文件可用性检查
    //数据库记录存在，不一定代表磁盘图片存在，因此这里再确认一下
    public boolean isAvatarFileAvailable(String storageKey){
        if(storageKey == null || !storageKey.startsWith("avatars/")){
            return false;
        }

        try{
            Path target = rootDir.resolve(storageKey).normalize();

            //路径必须位于配置的存储根目录内
            if(!target.startsWith(rootDir)){
                return false;
            }

            Path actualPath = target.toRealPath();

            return actualPath.startsWith(rootDir)
                    &&Files.isRegularFile(actualPath)   //isRegular()检查目标是普通文件
                    &&Files.isReadable(actualPath);     //isReadable()检查程序能否读取。解析实际路径后再次检查所属目录，可以避免通过目录链接跳到存储根目录之外。
        }catch (IOException | InvalidPathException exception){
            return false;
        }
    }

    private BusinessException avatarContentUnavailable(){
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "AVATAR_CONTENT_UNAVAILABLE",
                "头像文件不可访问"
        );
    }

    //Service的读取方法
    public FileContent readAvatar(Long userId,Long fileId){
        if(fileId == null || fileId <= 0){
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FILE_ID",
                    "文件ID必须为整数"
            );
        }

        FileMetadata metadata =
                fileMetadataMapper.selectCurrentAvatarForRead(
                        fileId,
                        userId
                );

        if (metadata == null){
            throw avatarContentUnavailable();
        }

        String mimeType = metadata.getMime_type();

        boolean supportedType =
                "image/jpeg".equals(mimeType)
                        || "image/png".equals(mimeType)
                        || "image/webp".equals(mimeType);

        if (!supportedType
                || !isAvatarFileAvailable(metadata.getStorage_key())){
            throw avatarContentUnavailable();
        }

        try{
            Path actualPath = rootDir
                    .resolve(metadata.getStorage_key())
                    .normalize()
                    .toRealPath();

            if (!actualPath.startsWith(rootDir)){
                throw avatarContentUnavailable();
            }

            byte[] content;

            try(InputStream input = Files.newInputStream(actualPath)){
                content = input.readNBytes(
                        (int) MAX_AVATAR_SIZE + 1
                );
            }

            if (content.length == 0
                     || content.length>MAX_AVATAR_SIZE
                     || !Objects.equals(
                                metadata.getSize_bytes(),
                                (long)content.length
            )){
                throw avatarContentUnavailable();
            }

            return new FileContent(content,mimeType);
        }catch (IOException|InvalidPathException exception){
            log.warn("读取头像文件失败,fileId={}",fileId,exception);
            throw avatarContentUnavailable();
        }
    }
}

//路径就是描述“某个文件或目录在哪里”的地址，例如：D:\PetCure-data\\uploads指向一个目录，所以它是目录路径
//而D:\PetCure-data\\uploads\avatars\cat.png指向一张图片，所以它是文件路径
//两者都是路径，只是指向的对象不同

//父目录就是直接包含当前文件或目录的上一层文件夹，如cat.png的父目录就是avatar，avatars的父目录就是uploads

//绝对路径是从文件系统的根位置开始，完整描述位置的路径
//例如在当前的Windows环境中D:\PetCure-data\\uploads就是绝对路径
