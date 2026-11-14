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
        Path configuredDir = Path.of(
                fileStorageProperties.getRootDir()
        ).normalize();

        if(!configuredDir.isAbsolute()){
            throw new IllegalArgumentException(
                    "文件存储根目录必须使用绝对路径"
            );
        }

        try{
            //目录不存在时，连同父目录一起创建
            Files.createDirectories(configuredDir);

            //获取实际存在的目录路径，供后续保存文件使用
            this.rootDir = configuredDir.toRealPath();
        }catch(IOException exception){
            throw new IllegalArgumentException(
                    "无法准备文件存储目录，请检查路径和访问权限",
                    exception
            );
        }
    }
}
