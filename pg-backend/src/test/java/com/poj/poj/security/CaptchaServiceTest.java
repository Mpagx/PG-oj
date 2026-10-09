package com.poj.poj.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.jupiter.api.Assertions.*;
class CaptchaServiceTest {
    @Test void challengeIsSingleUseAndExpires() {
        CaptchaService service = new CaptchaService(true);
        MockHttpSession session = new MockHttpSession();
        String answer = service.issue(session);
        assertDoesNotThrow(() -> service.verify(session, answer));
        assertThrows(RuntimeException.class, () -> service.verify(session, answer));
        String expired = service.issue(session);
        session.setAttribute("captcha.deadline", 0L);
        assertThrows(RuntimeException.class, () -> service.verify(session, expired));
    }
}
