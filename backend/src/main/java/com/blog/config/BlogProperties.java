package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "blog")
public class BlogProperties {
    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Site site = new Site();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000L;
    }

    @Data
    public static class Upload {
        private String dir = "./uploads";
        private String urlPrefix = "/uploads";
    }

    @Data
    public static class Site {
        private String baseUrl = "http://localhost:5173";
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }
}
