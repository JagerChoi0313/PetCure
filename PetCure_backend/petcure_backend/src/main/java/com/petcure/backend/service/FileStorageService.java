package com.petcure.backend.service;

import com.petcure.backend.config.FileStorageProperties;
import com.petcure.backend.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

@Service
public class FileStorageService {

    //定义头像大小上限
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;//头像最大为5MB，单位为字节

    //读取头像时，最多允许2000万像素
    private static final long MAX_AVATAR_PIXELS = 20_000_000L;
    //文件实际保存的根目录
    private final Path rootDir;

    public FileStorageService(
            FileStorageProperties fileStorageProperties
    ){
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
}

//路径就是描述“某个文件或目录在哪里”的地址，例如：D:\PetCure-data\\uploads指向一个目录，所以它是目录路径
//而D:\PetCure-data\\uploads\avatars\cat.png指向一张图片，所以它是文件路径
//两者都是路径，只是指向的对象不同

//父目录就是直接包含当前文件或目录的上一层文件夹，如cat.png的父目录就是avatar，avatars的父目录就是uploads

//绝对路径是从文件系统的根位置开始，完整描述位置的路径
//例如在当前的Windows环境中D:\PetCure-data\\uploads就是绝对路径
