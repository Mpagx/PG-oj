package com.poj.poj.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

public final class Passwords {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);
    private Passwords() { }
    public static String encode(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new com.poj.poj.exception.BusinessException(com.poj.poj.common.ErrorCode.PARAMS_ERROR, "密码至少 8 位，UTF-8 长度不能超过 72 字节");
        }
        return ENCODER.encode(password);
    }
    public static boolean legacy(String hash) { return hash != null && hash.matches("[a-fA-F0-9]{32}"); }
    public static String format(String hash) {
        if (legacy(hash)) return "LEGACY_MD5";
        if (hash != null && hash.matches("^\\$2[aby]\\$\\d{2}\\$.+")) return "BCRYPT";
        return hash == null ? "MISSING" : "UNSUPPORTED";
    }
    public static boolean matches(String password, String hash) {
        if (password == null || hash == null || password.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        if (!legacy(hash)) return ENCODER.matches(password, hash);
        String old = DigestUtils.md5DigestAsHex(("yupi" + password).getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(old.getBytes(StandardCharsets.US_ASCII), hash.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
}
