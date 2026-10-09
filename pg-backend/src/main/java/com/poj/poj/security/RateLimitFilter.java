package com.poj.poj.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Production uses atomic Redis counters shared by all backend instances. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {
    private final ObjectProvider<StringRedisTemplate> redis;
    private final Map<String, long[]> local = new ConcurrentHashMap<>();
    private final boolean distributed;
    private final int loginLimit, registerLimit, submitLimit;
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return n", Long.class);
    public RateLimitFilter(ObjectProvider<StringRedisTemplate> redis,
            @Value("${app.rate-limit.redis:false}") boolean distributed,
            @Value("${app.rate-limit.login:10}") int loginLimit,
            @Value("${app.rate-limit.register:5}") int registerLimit,
            @Value("${app.rate-limit.submit:20}") int submitLimit) {
        this.redis = redis; this.distributed = distributed;
        this.loginLimit = loginLimit; this.registerLimit = registerLimit; this.submitLimit = submitLimit;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        int limit = 0;
        if ("/user/login".equals(path)) limit = loginLimit;
        else if ("/user/register".equals(path)) limit = registerLimit;
        else if ("/security/captcha".equals(path)) limit = 30;
        else if ("/question_submit/".equals(path) && "POST".equals(request.getMethod())) limit = submitLimit;
        else if ("/question_submit/custom-test".equals(path) && "POST".equals(request.getMethod())) limit = 10;
        else if ("/user/email/code".equals(path) && "POST".equals(request.getMethod())) limit = 5;
        else if ("/user/password/reset".equals(path) && "POST".equals(request.getMethod())) limit = 5;
        else if ("/admin/problem-import/zip".equals(path) && "POST".equals(request.getMethod())) limit = 10;
        else if ("/question-solution/save".equals(path) && "POST".equals(request.getMethod())) limit = 10;
        if (limit > 0) {
            long window = System.currentTimeMillis() / 60000;
            // Never trust client-supplied forwarding headers. The reverse proxy must overwrite them.
            String key = "poj:rate:" + path + ":" + request.getRemoteAddr() + ":" + window;
            try {
                long count;
                if (distributed) {
                    Long value = redis.getObject().execute(INCREMENT, java.util.Collections.singletonList(key), "120");
                    if (value == null) throw new IllegalStateException("Redis rate counter unavailable");
                    count = value;
                } else {
                    local.entrySet().removeIf(e -> e.getValue()[0] != window);
                    if (local.size() > 10000) { reject(response, 429, "请求过于频繁，请稍后再试"); return; }
                    long[] state = local.computeIfAbsent(key, ignored -> new long[]{window, 0});
                    synchronized (state) { count = ++state[1]; }
                }
                if (count > limit) { reject(response, 429, "请求过于频繁，请一分钟后再试"); return; }
            } catch (RuntimeException error) {
                reject(response, 503, "请求验证服务暂时不可用"); return;
            }
        }
        chain.doFilter(request, response);
    }
    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        if (status == 429) response.setHeader("Retry-After", "60");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + (status * 100) + ",\"message\":\"" + message + "\"}");
    }
}
