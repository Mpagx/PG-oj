package com.poj.poj.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.LoginUserVO;
import org.junit.jupiter.api.Test;

class UserLoginViewTest {
    @Test
    void loginViewContainsCanonicalUserName() {
        User user = new User();
        user.setId(2L);
        user.setUserName("AAAA");
        user.setUserRole("user");

        LoginUserVO result = new UserServiceImpl().getLoginUserVO(user);

        assertEquals("AAAA", result.getUserName());
        assertEquals("user", result.getUserRole());
    }
}
