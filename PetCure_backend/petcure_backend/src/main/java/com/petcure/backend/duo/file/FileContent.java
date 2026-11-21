package com.petcure.backend.duo.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class FileContent {

    private final byte[] data;//data:保存从磁盘读取的文件字节

    private final String mimeType;//mimeType:保存文件内容，例如image/Jpeg
}
//这个对象用于Service向Controller传递结果。下一步Contoller1会把字节直接写进HTTP响应