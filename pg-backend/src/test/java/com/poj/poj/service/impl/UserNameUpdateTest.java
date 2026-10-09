package com.poj.poj.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.mapper.UserMapper;
import com.poj.poj.model.dto.user.UserUpdateMyRequest;
import com.poj.poj.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Date;
import org.springframework.test.util.ReflectionTestUtils;

class UserNameUpdateTest {

    private UserMapper userMapper;
    private UserServiceImpl userService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUserName("AAAA");
        currentUser.setUserProfile("before");
        when(userMapper.selectById(1L)).thenReturn(currentUser);
    }

    @Test
    void rejectsDuplicateUserNameDuringRegistration() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> userService.userRegister("PGGG", "password123", "password123"));

        assertEquals("用户名已存在", error.getMessage());
    }

    @Test
    void changesUniqueUserNameAndStartsThirtyDayCooldown() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateMyRequest request = request();
        request.setUserName("new_name");
        userService.updateMyUser(request, currentUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("new_name", captor.getValue().getUserName());
        assertNotNull(captor.getValue().getUserNameUpdateTime());
        assertEquals("after", captor.getValue().getUserProfile());
    }

    @Test
    void rejectsUserNameChangeDuringCooldown() {
        currentUser.setUserNameUpdateTime(new Date());
        UserUpdateMyRequest request = request();
        request.setUserName("another_name");

        BusinessException error = assertThrows(BusinessException.class,
                () -> userService.updateMyUser(request, currentUser));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("30 天"));
    }

    private UserUpdateMyRequest request() {
        UserUpdateMyRequest request = new UserUpdateMyRequest();
        request.setUserProfile("after");
        return request;
    }
}
