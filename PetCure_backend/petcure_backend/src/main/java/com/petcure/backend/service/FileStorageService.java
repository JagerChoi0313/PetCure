package com.petcure.backend.service;

import com.petcure.backend.config.FileStorageProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileStorageService {

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
}

//路径就是描述“某个文件或目录在哪里”的地址，例如：D:\PetCure-data\\uploads指向一个目录，所以它是目录路径
//而D:\PetCure-data\\uploads\avatars\cat.png指向一张图片，所以它是文件路径
//两者都是路径，只是指向的对象不同

//父目录就是直接包含当前文件或目录的上一层文件夹，如cat.png的父目录就是avatar，avatars的父目录就是uploads

//绝对路径是从文件系统的根位置开始，完整描述位置的路径
//例如在当前的Windows环境中D:\PetCure-data\\uploads就是绝对路径
