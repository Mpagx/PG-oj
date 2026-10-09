package com.cookie.codesandbox.config;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
@Configuration
@Profile("prod")
public class ProductionConfigCheck {
    @Value("${codesandbox.token:}") private String token;
    @PostConstruct public void validate() {
        if (token == null || token.length() < 32 || token.startsWith("replace-")) {
            throw new IllegalStateException("生产沙箱需要至少 32 字符的独立随机令牌");
        }
    }
}
