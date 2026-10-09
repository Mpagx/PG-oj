package com.poj.poj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.poj.poj.model.dto.user.UserQueryRequest;
import com.poj.poj.model.dto.user.UserUpdateMyRequest;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.LoginUserVO;
import com.poj.poj.model.vo.UserVO;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;

/**
 * 用户服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userName      唯一用户名
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userName, String userPassword, String checkPassword, String userEmail);

    default long userRegister(String userName, String userPassword, String checkPassword) {
        return userRegister(userName, userPassword, checkPassword, null);
    }

    /**
     * 用户登录
     *
     * @param userName     唯一用户名
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userName, String userPassword, HttpServletRequest request);

    /** 更新当前用户资料；唯一用户名每 30 天最多修改一次。 */
    boolean updateMyUser(UserUpdateMyRequest request, User loginUser);

    /**
     * 用户登录（微信开放平台）
     *
     * @param wxOAuth2UserInfo 从微信获取的用户信息
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
