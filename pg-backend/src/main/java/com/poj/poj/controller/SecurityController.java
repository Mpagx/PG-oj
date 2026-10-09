package com.poj.poj.controller;

import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.ResultUtils;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/security")
public class SecurityController {
    private final com.poj.poj.security.CaptchaService captcha;
    public SecurityController(com.poj.poj.security.CaptchaService captcha) { this.captcha = captcha; }
    @GetMapping(value = "/captcha", produces = "image/png")
    public void captcha(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String answer = captcha.issue(request.getSession());
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(180, 50, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        java.security.SecureRandom random = new java.security.SecureRandom();
        graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, 180, 50);
        graphics.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 28));
        for (int i = 0; i < 6; i++) {
            graphics.setColor(new java.awt.Color(random.nextInt(140), random.nextInt(140), random.nextInt(140)));
            graphics.drawString(answer.substring(i, i + 1), 10 + i * 27, 32 + random.nextInt(9));
        }
        for (int i = 0; i < 15; i++) {
            graphics.setColor(new java.awt.Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            graphics.drawLine(random.nextInt(180), random.nextInt(50), random.nextInt(180), random.nextInt(50));
        }
        graphics.dispose(); response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store");
        javax.imageio.ImageIO.write(image, "png", response.getOutputStream());
    }
    @GetMapping("/csrf")
    public BaseResponse<String> csrf(CsrfToken token) { return ResultUtils.success(token.getToken()); }
}
