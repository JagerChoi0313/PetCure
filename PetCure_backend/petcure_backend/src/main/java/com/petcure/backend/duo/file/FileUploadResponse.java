package com.petcure.backend.duo.file;

import lombok.AllArgsConstructor;
import lombok.Getter;

//创建上传响应对象
@Getter
@AllArgsConstructor
public class FileUploadResponse {

        private final Long fileId;

        private final String mimeType;

        private final Long sizeBytes;


}
