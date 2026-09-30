package com.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature pack 集成测试：依赖 application-local.yml 中的真实 MySQL。
 * Redis 不可用时缓存降级，API 测试仍应通过。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "blog.security.captcha-enabled=false",
        "blog.security.encrypt-response=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeaturePackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String memberToken;
    private static Long articleId;
    private static Long categoryId;
    private static String phone;

    @Test
    @Order(1)
    void registerAndLoginMember() throws Exception {
        phone = "1" + String.format("%010d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 10_000_000_000L));
        String body = """
                {"phone":"%s","password":"test1234","nickname":"测试会员","email":"t@example.com"}
                """.formatted(phone);

        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        JsonNode regData = objectMapper.readTree(reg.getResponse().getContentAsString()).path("data");
        memberToken = regData.path("token").asText();
        assertFalse(memberToken.isBlank());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"test1234\"}".formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @Order(2)
    void updateProfile() throws Exception {
        assumeMember();
        mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"改名会员\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("改名会员"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.avatar").isString());
    }

    @Test
    @Order(3)
    void prepareArticleContext() throws Exception {
        MvcResult articles = mockMvc.perform(get("/api/public/articles").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode records = objectMapper.readTree(articles.getResponse().getContentAsString())
                .path("data").path("records");
        assertTrue(records.isArray() && records.size() > 0, "需要至少一篇已发布文章");
        articleId = records.get(0).path("id").asLong();
        categoryId = records.get(0).path("categoryId").asLong(0);
        if (categoryId == 0) {
            MvcResult cats = mockMvc.perform(get("/api/public/categories")).andReturn();
            JsonNode catArr = objectMapper.readTree(cats.getResponse().getContentAsString()).path("data");
            if (catArr.isArray() && catArr.size() > 0) {
                categoryId = catArr.get(0).path("id").asLong();
            }
        }
        assertNotNull(articleId);
    }

    @Test
    @Order(4)
    void favoriteArticle() throws Exception {
        assumeMember();
        assertNotNull(articleId);
        mockMvc.perform(post("/api/auth/favorites/" + articleId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/auth/favorites")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(5)
    void readingProgress() throws Exception {
        assumeMember();
        if (categoryId == null || categoryId == 0) {
            return;
        }
        mockMvc.perform(post("/api/auth/reading-progress")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":%d,\"articleId\":%d}".formatted(categoryId, articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/auth/reading-progress")
                        .param("categoryId", String.valueOf(categoryId))
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.articleId").value(articleId));
    }

    @Test
    @Order(6)
    void fulltextSearchEndpoint() throws Exception {
        mockMvc.perform(get("/api/public/search").param("q", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(7)
    void publicAnnouncements() throws Exception {
        mockMvc.perform(get("/api/public/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(8)
    void adminLogin() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.mustChangePassword").exists());
    }

    @Test
    @Order(9)
    void rateLimitDoesNotBreakNormalLogin() throws Exception {
        assumeMember();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"test1234\"}".formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void assumeMember() {
        assertNotNull(memberToken, "member token required — register test must run first");
    }
}
