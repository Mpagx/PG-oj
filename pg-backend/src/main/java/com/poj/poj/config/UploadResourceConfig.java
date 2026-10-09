package com.poj.poj.config;

import com.poj.poj.service.AvatarStorageService;
import javax.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    @Resource
    private AvatarStorageService avatarStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(avatarStorageService.getResourceLocation())
                .setCachePeriod(86400);
    }
}
