package com.blog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final BlogProperties blogProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = blogProperties.getCors().getAllowedOrigins().toArray(new String[0]);
        registry.addMapping("/**")
                .allowedOrigins(origins.length > 0 ? origins : new String[]{"*"})
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Paths.get(blogProperties.getUpload().getDir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(blogProperties.getUpload().getUrlPrefix() + "/**")
                .addResourceLocations(dir.endsWith("/") ? dir : dir + "/");
    }
}
