package com.poj.poj.security;

import com.poj.poj.mapper.UserMapper;
import com.poj.poj.model.entity.User;
import com.poj.poj.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserAuthenticationTest {
    @Test void successfulLegacyLoginUpgradesPasswordAndRotatesSession() {
        UserMapper mapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        User user = new User(); user.setId(1L); user.setUserName("legacy_user"); user.setUserRole("user");
        user.setUserPassword(org.springframework.util.DigestUtils.md5DigestAsHex("yupitest-password".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(mapper.selectOne(any())).thenReturn(user); when(mapper.update(isNull(), any())).thenReturn(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        String oldSession = request.getSession().getId();
        assertNotNull(service.userLogin("legacy_user", "test-password", request));
        assertNotEquals(oldSession, request.getSession().getId());
        assertTrue(user.getUserPassword().startsWith("$2"));
        verify(mapper).update(isNull(), any());
    }
    @Test void blockedUserCannotLogIn() {
        UserMapper mapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl(); ReflectionTestUtils.setField(service, "baseMapper", mapper);
        User user = new User(); user.setId(1L); user.setUserRole("ban"); user.setUserPassword(Passwords.encode("test-password"));
        when(mapper.selectOne(any())).thenReturn(user);
        assertThrows(RuntimeException.class, () -> service.userLogin("banned_user", "test-password", new MockHttpServletRequest()));
    }
}
