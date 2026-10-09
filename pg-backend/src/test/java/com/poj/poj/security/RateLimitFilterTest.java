package com.poj.poj.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {
    @Test void blocksRepeatedLoginButAllowsDifferentIp() throws Exception {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        RateLimitFilter filter = new RateLimitFilter(provider, false, 2, 1, 1);
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/login");
            request.setServletPath("/user/login"); request.setRemoteAddr("127.0.0.2");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> { });
            assertEquals(i == 2 ? 429 : 200, response.getStatus());
        }
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/login");
        request.setServletPath("/user/login"); request.setRemoteAddr("127.0.0.3");
        MockHttpServletResponse response = new MockHttpServletResponse(); filter.doFilter(request, response, (req, res) -> { });
        assertEquals(200, response.getStatus());
    }
    @Test void redisOutageDoesNotBypassLimits() throws Exception {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenThrow(new IllegalStateException("Redis down"));
        RateLimitFilter filter = new RateLimitFilter(provider, true, 1, 1, 1);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/login"); request.setServletPath("/user/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Should not bypass limiter"));
        assertEquals(503, response.getStatus());
    }
}
