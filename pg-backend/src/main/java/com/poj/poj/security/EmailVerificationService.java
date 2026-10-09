package com.poj.poj.security;

import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class EmailVerificationService {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> PURPOSES = Set.of("REGISTER", "BIND", "RESET");
    private static final DefaultRedisScript<String> TAKE = new DefaultRedisScript<>(
            "local v=redis.call('GET',KEYS[1]); if v then redis.call('DEL',KEYS[1]) end; return v",
            String.class);
    private final ObjectProvider<JavaMailSender> mailSender;
    private final ObjectProvider<StringRedisTemplate> redis;
    private final Map<String, Entry> local = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final boolean enabled;
    private final boolean distributed;
    private final String from;
    private final String secret;

    public EmailVerificationService(ObjectProvider<JavaMailSender> mailSender,
            ObjectProvider<StringRedisTemplate> redis,
            @Value("${app.email.enabled:false}") boolean enabled,
            @Value("${app.email.redis:false}") boolean distributed,
            @Value("${app.email.from:}") String from,
            @Value("${app.email.code-secret:development-only-change-me}") String secret) {
        this.mailSender = mailSender;
        this.redis = redis;
        this.enabled = enabled;
        this.distributed = distributed;
        this.from = from;
        this.secret = secret;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String normalize(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        return normalized;
    }

    public void send(String purpose, String email, long subjectId) {
        requireEnabled();
        purpose = normalizePurpose(purpose);
        email = normalize(email);
        String key = key(purpose, email, subjectId);
        String cooldownKey = key + ":cooldown";
        String code = String.format("%06d", random.nextInt(1_000_000));
        String digest = digest(key, code);
        reserve(key, cooldownKey, digest);
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || from.isBlank()) {
                throw new IllegalStateException("SMTP is not configured");
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("Cookie OJ 验证码");
            message.setText("你的 Cookie OJ 验证码是：" + code + "。10 分钟内有效，请勿转发给他人。");
            sender.send(message);
        } catch (RuntimeException error) {
            remove(key, cooldownKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码邮件发送失败，请稍后重试");
        }
    }

    public void verify(String purpose, String email, long subjectId, String code) {
        requireEnabled();
        purpose = normalizePurpose(purpose);
        email = normalize(email);
        if (code == null || !code.matches("^\\d{6}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱验证码格式不正确");
        }
        String key = key(purpose, email, subjectId);
        String expected = take(key);
        boolean matches = expected != null && MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                digest(key, code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!matches) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱验证码错误或已过期");
        }
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮件服务尚未配置");
        }
    }

    private String normalizePurpose(String purpose) {
        String normalized = purpose == null ? "" : purpose.trim().toUpperCase(Locale.ROOT);
        if (!PURPOSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱验证码用途无效");
        }
        return normalized;
    }

    private String key(String purpose, String email, long subjectId) {
        return "poj:email-code:" + purpose + ":" + subjectId + ":" + DigestUtils.sha256Hex(email);
    }

    private String digest(String key, String code) {
        return DigestUtils.sha256Hex(secret + ":" + key + ":" + code);
    }

    private void reserve(String key, String cooldownKey, String digest) {
        if (distributed) {
            StringRedisTemplate template = redis.getIfAvailable();
            if (template == null) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码服务不可用");
            Boolean reserved = template.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofSeconds(60));
            if (!Boolean.TRUE.equals(reserved)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码发送过于频繁，请一分钟后再试");
            }
            template.opsForValue().set(key, digest, Duration.ofMinutes(10));
            return;
        }
        long now = System.currentTimeMillis();
        local.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        Entry existing = local.get(cooldownKey);
        if (existing != null && existing.expiresAt > now) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码发送过于频繁，请一分钟后再试");
        }
        local.put(cooldownKey, new Entry("1", now + 60_000));
        local.put(key, new Entry(digest, now + 600_000));
    }

    private String take(String key) {
        if (distributed) {
            StringRedisTemplate template = redis.getIfAvailable();
            if (template == null) return null;
            return template.execute(TAKE, java.util.Collections.singletonList(key));
        }
        Entry entry = local.remove(key);
        return entry != null && entry.expiresAt >= System.currentTimeMillis() ? entry.value : null;
    }

    private void remove(String key, String cooldownKey) {
        if (distributed) {
            StringRedisTemplate template = redis.getIfAvailable();
            if (template != null) template.delete(java.util.List.of(key, cooldownKey));
        } else {
            local.remove(key);
            local.remove(cooldownKey);
        }
    }

    private static class Entry {
        private final String value;
        private final long expiresAt;
        private Entry(String value, long expiresAt) { this.value = value; this.expiresAt = expiresAt; }
    }
}
