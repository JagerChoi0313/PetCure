package com.petcure.backend.controller;

import com.petcure.backend.common.ApiResponse;
import com.petcure.backend.duo.file.FileUploadResponse;
import com.petcure.backend.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService){
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(
            value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )

    public ApiResponse<FileUploadResponse>uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file")MultipartFile file
            ){
        //JwtConfig已经检查过用户ID，这里读取认证后的身份
        Long userId = Long.valueOf(jwt.getSubject());

        return ApiResponse.success(
                fileStorageService.uploadAvatar(userId,file)
        );
    }
}
