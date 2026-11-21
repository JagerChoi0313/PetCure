package com.petcure.backend.duo.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class FileContent {

    private final byte[] data;

    private final String mimeType;
}
