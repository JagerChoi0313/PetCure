package com.petcure.backend.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated     //在绑定配置时执行校验
@Configuration
@ConfigurationProperties(prefix = "app.storage")    //读取app.storage下的配置并绑定到属性
public class FileStorageProperties {

    @NotBlank(message = "文件存储根目录不能为空")
    private String rootDir;
}
