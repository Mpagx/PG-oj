package com.poj.poj.config;

import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("prod")
public class ProductionConfigCheck {
    private final Environment environment;
    public ProductionConfigCheck(Environment environment) { this.environment = environment; }
    @PostConstruct
    public void validate() {
        for (String key : new String[]{"spring.datasource.password", "spring.redis.password", "codesandbox.token", "app.cors.allowed-origins"}) {
            String value = environment.getProperty(key, "");
            if (value.trim().isEmpty() || "123456".equals(value) || value.contains("*")) {
                throw new IllegalStateException("生产配置缺失或使用不安全默认值: " + key);
            }
        }
        if (!"remote".equals(environment.getProperty("codesandbox.type"))) throw new IllegalStateException("生产环境必须使用真实沙箱");
        String token = environment.getProperty("codesandbox.token", "");
        if (token.length() < 32 || token.startsWith("replace-")) throw new IllegalStateException("生产沙箱令牌必须至少 32 字符且不能使用示例值");
        if (!"redis".equals(environment.getProperty("spring.session.store-type"))) throw new IllegalStateException("生产环境必须使用 Redis Session");
        if (!environment.getProperty("app.email.enabled", Boolean.class, false)) throw new IllegalStateException("生产环境必须启用邮箱验证");
        for (String key : new String[]{"spring.mail.host", "spring.mail.username", "spring.mail.password", "app.email.from"}) {
            if (environment.getProperty(key, "").trim().isEmpty()) throw new IllegalStateException("生产邮件配置缺失: " + key);
        }
        String emailSecret = environment.getProperty("app.email.code-secret", "");
        if (emailSecret.length() < 32 || emailSecret.startsWith("development-")) throw new IllegalStateException("邮箱验证码密钥必须至少 32 字符且不能使用开发默认值");
    }
}
