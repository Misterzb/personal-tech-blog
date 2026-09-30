package com.blog.service;

import com.blog.config.BlogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 生成可爱科技风默认头像（本地 SVG，不依赖外网）。
 */
@Service
@RequiredArgsConstructor
public class AvatarService {

    private final BlogProperties blogProperties;

    public String createDefaultAvatar(Long memberId, String seed) {
        int hash = Math.abs((seed == null ? "member" : seed).hashCode());
        String[] palettes = {
                "#0EA5E9|#0369A1|#E0F2FE",
                "#8B5CF6|#5B21B6|#EDE9FE",
                "#10B981|#047857|#D1FAE5",
                "#F59E0B|#B45309|#FEF3C7",
                "#EC4899|#9D174D|#FCE7F3",
                "#14B8A6|#0F766E|#CCFBF1"
        };
        String[] parts = palettes[hash % palettes.length].split("\\|");
        String primary = parts[0];
        String dark = parts[1];
        String soft = parts[2];
        int eye = 18 + (hash % 6);
        int antenna = 8 + (hash % 8);

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" width="128" height="128">
                  <defs>
                    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%%" stop-color="%s"/>
                      <stop offset="100%%" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="128" height="128" rx="28" fill="%s"/>
                  <circle cx="64" cy="22" r="%d" fill="%s"/>
                  <rect x="60" y="28" width="8" height="14" rx="4" fill="%s"/>
                  <rect x="28" y="42" width="72" height="62" rx="22" fill="url(#g)"/>
                  <circle cx="48" cy="68" r="%d" fill="#0F172A"/>
                  <circle cx="80" cy="68" r="%d" fill="#0F172A"/>
                  <circle cx="52" cy="65" r="4" fill="#F8FAFC"/>
                  <circle cx="84" cy="65" r="4" fill="#F8FAFC"/>
                  <rect x="50" y="86" width="28" height="8" rx="4" fill="#0F172A" opacity="0.75"/>
                  <rect x="18" y="58" width="12" height="28" rx="6" fill="%s"/>
                  <rect x="98" y="58" width="12" height="28" rx="6" fill="%s"/>
                </svg>
                """.formatted(primary, dark, soft, antenna, dark, dark, eye / 2, eye / 2, primary, primary);

        try {
            Path dir = Paths.get(blogProperties.getUpload().getDir(), "avatars").toAbsolutePath();
            Files.createDirectories(dir);
            String name = "m" + memberId + ".svg";
            Files.writeString(dir.resolve(name), svg, StandardCharsets.UTF_8);
            return blogProperties.getUpload().getUrlPrefix() + "/avatars/" + name;
        } catch (IOException e) {
            // 极端情况返回 data URI，避免注册失败
            return "data:image/svg+xml;utf8," + java.net.URLEncoder.encode(svg, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        }
    }
}
