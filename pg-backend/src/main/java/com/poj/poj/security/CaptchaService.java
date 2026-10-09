package com.poj.poj.security;

import java.security.SecureRandom;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;

@Service
public class CaptchaService {
    private final boolean enabled;
    private final SecureRandom random = new SecureRandom();
    public CaptchaService(@Value("${app.captcha.enabled:true}") boolean enabled) { this.enabled = enabled; }
    public String issue(HttpSession session) {
        String answer = String.format("%06d", random.nextInt(1000000));
        session.setAttribute("captcha.answer", answer);
        session.setAttribute("captcha.deadline", System.currentTimeMillis() + 120000);
        return answer;
    }
    public void verify(HttpSession session, String answer) {
        if (!enabled) return;
        Object expected = session.getAttribute("captcha.answer");
        Object deadline = session.getAttribute("captcha.deadline");
        session.removeAttribute("captcha.answer");
        session.removeAttribute("captcha.deadline");
        if (!(deadline instanceof Long) || (Long) deadline < System.currentTimeMillis()
                || expected == null || !expected.equals(answer)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期，请刷新验证码");
        }
    }
}
