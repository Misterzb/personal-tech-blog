package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 32, message = "昵称长度需 1-32 位")
    private String nickname;

    @Size(max = 128, message = "邮箱过长")
    private String email;
}
