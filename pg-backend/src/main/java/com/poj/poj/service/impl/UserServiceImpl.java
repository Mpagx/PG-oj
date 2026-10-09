package com.poj.poj.service.impl;

import static com.poj.poj.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.constant.CommonConstant;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.mapper.UserMapper;
import com.poj.poj.model.dto.user.UserQueryRequest;
import com.poj.poj.model.dto.user.UserUpdateMyRequest;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.enums.UserRoleEnum;
import com.poj.poj.model.vo.LoginUserVO;
import com.poj.poj.model.vo.UserVO;
import com.poj.poj.service.UserService;
import com.poj.poj.utils.SqlUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "yupi";

    @Override
    public long userRegister(String userName, String userPassword, String checkPassword, String userEmail) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userName, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        userName = normalizeAndValidateUserName(userName);
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (this) {
            // 登录用户名不能重复；数据库还有唯一索引作为并发场景的最终保障
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userName", userName);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
            }
            if (StringUtils.isNotBlank(userEmail)) {
                QueryWrapper<User> emailQuery = new QueryWrapper<>();
                emailQuery.eq("userEmail", userEmail);
                if (this.baseMapper.selectCount(emailQuery) > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已绑定其他用户");
                }
            }
            // 2. 加密
            String encryptPassword = com.poj.poj.security.Passwords.encode(userPassword);
            // 3. 插入数据
            User user = new User();
            user.setUserName(userName);
            user.setUserPassword(encryptPassword);
            user.setUserEmail(StringUtils.trimToNull(userEmail));
            if (StringUtils.isNotBlank(userEmail)) user.setEmailVerifiedAt(new Date());
            try {
                boolean saveResult = this.save(user);
                if (!saveResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
                }
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userName, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userName, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        userName = userName.trim();
        if (userName.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userName", userName);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 对客户端保持统一提示，避免泄露用户名是否存在；服务端日志只记录安全的失败类别。
        if (user == null) {
            log.info("user login failed: username not found");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        if (!com.poj.poj.security.Passwords.matches(userPassword, user.getUserPassword())) {
            log.info("user login failed: password mismatch, userId={}, passwordFormat={}",
                    user.getId(), com.poj.poj.security.Passwords.format(user.getUserPassword()));
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        if (UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
        }
        if (com.poj.poj.security.Passwords.legacy(user.getUserPassword())) {
            String upgraded = com.poj.poj.security.Passwords.encode(userPassword);
            this.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                    .eq("id", user.getId()).eq("userPassword", user.getUserPassword()).set("userPassword", upgraded));
            user.setUserPassword(upgraded);
        }
        request.getSession();
        request.changeSessionId();
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                String digest = DigestUtils.md5DigestAsHex(unionId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                user.setUserName("wx_" + digest.substring(0, 20));
                user.setUserPassword(com.poj.poj.security.Passwords.encode(UUID.randomUUID().toString()));
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            request.getSession();
            request.changeSessionId();
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        User persistedUser = this.getById(userId);
        if (persistedUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long sessionPasswordVersion = currentUser.getPasswordChangedAt() == null
                ? null : currentUser.getPasswordChangedAt().getTime();
        Long persistedPasswordVersion = persistedUser.getPasswordChangedAt() == null
                ? null : persistedUser.getPasswordChangedAt().getTime();
        if (!java.util.Objects.equals(sessionPasswordVersion, persistedPasswordVersion)) {
            request.getSession().invalidate();
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "密码已修改，请重新登录");
        }
        currentUser = persistedUser;
        if (UserRoleEnum.BAN.getValue().equals(currentUser.getUserRole())) {
            request.getSession().invalidate();
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封");
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().invalidate();
        return true;
    }

    @Override
    public synchronized boolean updateMyUser(UserUpdateMyRequest request, User loginUser) {
        User persisted = this.getById(loginUser.getId());
        if (persisted == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        User update = new User();
        update.setId(persisted.getId());
        if (request.getUserProfile() != null) {
            update.setUserProfile(StringUtils.trimToEmpty(request.getUserProfile()));
        }
        if (request.getUserAvatar() != null) {
            update.setUserAvatar(request.getUserAvatar().trim());
        }
        String requestedName = StringUtils.trimToNull(request.getUserName());
        if (requestedName != null && !requestedName.equals(persisted.getUserName())) {
            requestedName = normalizeAndValidateUserName(requestedName);
            Date lastChanged = persisted.getUserNameUpdateTime();
            Date nextAllowed = lastChanged == null ? null : Date.from(lastChanged.toInstant().plus(30, java.time.temporal.ChronoUnit.DAYS));
            if (nextAllowed != null && new Date().before(nextAllowed)) {
                String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(nextAllowed);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户名每 30 天只能修改一次，下次可修改时间：" + date);
            }
            if (this.count(new QueryWrapper<User>().eq("userName", requestedName).ne("id", persisted.getId())) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
            }
            update.setUserName(requestedName);
            update.setUserNameUpdateTime(new Date());
        }
        try {
            return this.updateById(update);
        } catch (DuplicateKeyException error) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }
    }

    private String normalizeAndValidateUserName(String userName) {
        String normalized = userName.trim();
        if (normalized.isEmpty() || normalized.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度必须为 1 到 32 个字符");
        }
        if (!normalized.matches("^[\\p{L}\\p{N}_-]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名只能包含文字、数字、下划线和短横线");
        }
        return normalized;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
