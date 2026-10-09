package com.poj.poj.security;

import com.poj.poj.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    @Test
    void codeIsOneTimeAndNeverReturnedByTheApi() {
        JavaMailSender sender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> mailProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked") ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
        when(mailProvider.getIfAvailable()).thenReturn(sender);
        EmailVerificationService service = new EmailVerificationService(
                mailProvider, redisProvider, true, false, "no-reply@example.com",
                "test-secret-with-at-least-thirty-two-characters");

        service.send("REGISTER", "User@Example.com", 0L);
        org.mockito.ArgumentCaptor<SimpleMailMessage> message =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        Matcher matcher = Pattern.compile("(\\d{6})").matcher(message.getValue().getText());
        if (!matcher.find()) throw new AssertionError("verification code missing from email");
        String code = matcher.group(1);

        assertDoesNotThrow(() -> service.verify("REGISTER", "user@example.com", 0L, code));
        assertThrows(BusinessException.class,
                () -> service.verify("REGISTER", "user@example.com", 0L, code));
    }
}
