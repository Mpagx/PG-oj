package com.poj.poj.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class WebSecurityConfig {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokens.setCookiePath("/");
        http.cors().and().csrf().csrfTokenRepository(tokens)
                .and().authorizeRequests().anyRequest().permitAll()
                .and().formLogin().disable().httpBasic().disable().logout().disable()
                .exceptionHandling().accessDeniedHandler((request, response, error) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":40300,\"message\":\"请求验证已失效，请刷新页面\"}");
                });
        return http.build();
    }
}
