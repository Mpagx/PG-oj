package com.poj.poj.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PasswordsTest {
    @Test void verifiesLegacyAndSaltedBcryptWithLengthBounds() {
        String password = "test-password-123";
        String legacy = org.springframework.util.DigestUtils.md5DigestAsHex(("yupi" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(Passwords.matches(password, legacy));
        assertFalse(Passwords.matches("wrong", legacy));
        assertEquals("LEGACY_MD5", Passwords.format(legacy));
        String hash = Passwords.encode(password);
        assertFalse(Passwords.legacy(hash)); assertTrue(Passwords.matches(password, hash));
        assertEquals("BCRYPT", Passwords.format(hash));
        assertEquals("MISSING", Passwords.format(null));
        assertEquals("UNSUPPORTED", Passwords.format("plaintext-password"));
        assertNotEquals(hash, Passwords.encode(password));
        assertThrows(RuntimeException.class, () -> Passwords.encode("short"));
        assertThrows(RuntimeException.class, () -> Passwords.encode("a".repeat(73)));
    }
}
