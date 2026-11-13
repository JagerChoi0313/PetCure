package com.petcure.backend.duo.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserAvatarRequest {

    @NotNull(message = "请选择头像文件")
    @Positive(message = "头像文件ID必须为正数")
    private Long field;
}
