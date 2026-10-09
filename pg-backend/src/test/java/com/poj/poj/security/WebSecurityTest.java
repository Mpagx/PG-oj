package com.poj.poj.security;

import com.poj.poj.controller.SecurityController;
import com.poj.poj.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityController.class)
@Import({WebSecurityConfig.class, CorsConfig.class})
@org.springframework.test.context.ContextConfiguration(classes = {SecurityController.class, WebSecurityConfig.class, CorsConfig.class})
class WebSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean CaptchaService captcha;
    @Test void allowsDevelopmentFrontendOn8082() throws Exception {
        mvc.perform(get("/security/csrf").header("Origin", "http://localhost:8082"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8082"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    @Test void captchaReturnsDecodablePng() throws Exception {
        org.mockito.Mockito.when(captcha.issue(org.mockito.ArgumentMatchers.any())).thenReturn("123456");
        byte[] bytes = mvc.perform(get("/security/captcha"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        org.junit.jupiter.api.Assertions.assertNotNull(image);
        org.junit.jupiter.api.Assertions.assertEquals(180, image.getWidth());
        org.junit.jupiter.api.Assertions.assertEquals(50, image.getHeight());
    }
    @Test void rejectsMissingCsrfAndUntrustedOrigins() throws Exception {
        mvc.perform(post("/security/csrf")).andExpect(status().isForbidden());
        mvc.perform(get("/security/csrf").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/security/csrf")).andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN")).andExpect(jsonPath("$.data").isNotEmpty());
    }
}
