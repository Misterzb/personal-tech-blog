package com.blog.security;

import com.blog.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 将 /api/** 的 ApiResponse.data 加密为 Base64(AES-GCM)。
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class EncryptedResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ResponseCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return cryptoService.enabled();
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof ApiResponse<?> api)) {
            return body;
        }
        if (Boolean.TRUE.equals(api.getEncrypted())) {
            return body;
        }
        String path = request.getURI().getPath();
        if (path == null || !path.startsWith("/api/")) {
            return body;
        }
        try {
            String plain = objectMapper.writeValueAsString(api.getData());
            String cipher = cryptoService.encryptToBase64(plain);
            ApiResponse<String> encrypted = new ApiResponse<>(api.getCode(), api.getMessage(), cipher);
            encrypted.setEncrypted(true);
            response.getHeaders().set("X-Content-Encrypted", "aes-gcm");
            return encrypted;
        } catch (Exception e) {
            log.error("Encrypt response failed: {}", e.getMessage());
            return body;
        }
    }
}
