package com.blog.service;

import com.blog.common.BusinessException;
import com.blog.config.BlogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final BlogProperties blogProperties;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 图片");
        }
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        try {
            Path dir = Paths.get(blogProperties.getUpload().getDir()).toAbsolutePath();
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(name);
            file.transferTo(target.toFile());
            return blogProperties.getUpload().getUrlPrefix() + "/" + name;
        } catch (IOException e) {
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }
}
