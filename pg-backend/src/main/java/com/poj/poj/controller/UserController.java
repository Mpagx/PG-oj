package com.poj.poj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.poj.poj.annotation.AuthCheck;
import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.DeleteRequest;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.common.ResultUtils;
import com.poj.poj.config.WxOpenConfig;
import com.poj.poj.constant.UserConstant;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.exception.ThrowUtils;
import com.poj.poj.model.dto.user.UserAddRequest;
import com.poj.poj.model.dto.user.UserLoginRequest;
import com.poj.poj.model.dto.user.UserQueryRequest;
import com.poj.poj.model.dto.user.UserRegisterRequest;
import com.poj.poj.model.dto.user.UserUpdateMyRequest;
import com.poj.poj.model.dto.user.UserUpdateRequest;
import com.poj.poj.model.dto.user.EmailCodeRequest;
import com.poj.poj.model.dto.user.EmailBindRequest;
import com.poj.poj.model.dto.user.PasswordResetRequest;
import com.poj.poj.model.dto.user.AdminPasswordResetRequest;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.LoginUserVO;
import com.poj.poj.model.vo.UserVO;
import com.poj.poj.model.vo.AdminUserVO;
import com.poj.poj.service.UserService;
import com.poj.poj.service.AvatarStorageService;

import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static com.poj.poj.constant.UserConstant.USER_LOGIN_STATE;
import static com.poj.poj.service.impl.UserServiceImpl.SALT;

/**
 * 用户接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private com.poj.poj.security.CaptchaService captchaService;
    @Resource
    private com.poj.poj.security.EmailVerificationService emailVerificationService;
    @Resource
    private AvatarStorageService avatarStorageService;

    @Resource
    private WxOpenConfig wxOpenConfig;

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest req, HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userName = req.getUserName();
        String userPassword = req.getUserPassword();
        String checkPassword = req.getCheckPassword();

        if (StringUtils.isAnyBlank(userName, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名、密码、确认密码不能为空");
        }

        if (!emailVerificationService.isEnabled()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮箱验证服务尚未配置，暂时无法注册");
        }
        String userEmail = emailVerificationService.normalize(req.getUserEmail());
        emailVerificationService.verify("REGISTER", userEmail, 0L, req.getEmailCode());
        long result = userService.userRegister(
                userName,
                userPassword,
                checkPassword,
                userEmail
        );

        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userName = userLoginRequest.getUserName();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userName, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        captchaService.verify(request.getSession(), userLoginRequest.getCaptchaAnswer());
        LoginUserVO loginUserVO = userService.userLogin(userName, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /** 当前环境是否已启用 SMTP 邮箱验证。 */
    @GetMapping("/email/status")
    public BaseResponse<Boolean> emailStatus() {
        return ResultUtils.success(emailVerificationService.isEnabled());
    }

    /** 发送注册、绑定或找回密码验证码。 */
    @PostMapping("/email/code")
    public BaseResponse<Boolean> sendEmailCode(@RequestBody EmailCodeRequest body, HttpServletRequest request) {
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String purpose = StringUtils.upperCase(StringUtils.trimToEmpty(body.getPurpose()));
        String email = emailVerificationService.normalize(body.getEmail());
        long subjectId;
        if ("BIND".equals(purpose)) {
            User loginUser = userService.getLoginUser(request);
            subjectId = loginUser.getId();
            User duplicate = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("userEmail", email).ne("id", subjectId));
            if (duplicate != null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已绑定其他用户");
        } else if ("REGISTER".equals(purpose)) {
            subjectId = 0L;
            if (userService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("userEmail", email)) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已绑定其他用户");
            }
        } else if ("RESET".equals(purpose)) {
            captchaService.verify(request.getSession(), body.getCaptchaAnswer());
            User target = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("userEmail", email).isNotNull("emailVerifiedAt"));
            // 始终返回成功，避免向未登录者泄露邮箱是否注册。
            if (target == null) return ResultUtils.success(true);
            subjectId = target.getId();
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱验证码用途无效");
        }
        emailVerificationService.send(purpose, email, subjectId);
        return ResultUtils.success(true);
    }

    /** 当前用户绑定或更换已验证邮箱。 */
    @PostMapping("/email/bind")
    public BaseResponse<Boolean> bindEmail(@RequestBody EmailBindRequest body, HttpServletRequest request) {
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String email = emailVerificationService.normalize(body.getEmail());
        emailVerificationService.verify("BIND", email, loginUser.getId(), body.getCode());
        User duplicate = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("userEmail", email).ne("id", loginUser.getId()));
        if (duplicate != null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已绑定其他用户");
        User update = new User();
        update.setId(loginUser.getId());
        update.setUserEmail(email);
        update.setEmailVerifiedAt(new java.util.Date());
        try {
            return ResultUtils.success(userService.updateById(update));
        } catch (org.springframework.dao.DuplicateKeyException error) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已绑定其他用户");
        }
    }

    /** 使用已验证邮箱重置密码；成功后旧登录态会在下一次请求时失效。 */
    @PostMapping("/password/reset")
    public BaseResponse<Boolean> resetPassword(@RequestBody PasswordResetRequest body) {
        if (body == null || StringUtils.isAnyBlank(body.getEmail(), body.getCode(),
                body.getNewPassword(), body.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请完整填写重置信息");
        }
        if (!body.getNewPassword().equals(body.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        String email = emailVerificationService.normalize(body.getEmail());
        User target = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("userEmail", email).isNotNull("emailVerifiedAt"));
        if (target == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        emailVerificationService.verify("RESET", email, target.getId(), body.getCode());
        String password = com.poj.poj.security.Passwords.encode(body.getNewPassword());
        boolean updated = userService.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                .eq("id", target.getId())
                .set("userPassword", password)
                .set("passwordChangedAt", new java.util.Date()));
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "密码重置失败");
        return ResultUtils.success(true);
    }

    /** 管理员只能向用户的已验证邮箱发送重置码，不能读取或指定用户密码。 */
    @PostMapping("/password/reset/admin-send")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminSendPasswordReset(@RequestBody AdminPasswordResetRequest body) {
        if (body == null || body.getUserId() == null || body.getUserId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User target = userService.getById(body.getUserId());
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        if (target.getEmailVerifiedAt() == null || StringUtils.isBlank(target.getUserEmail())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户尚未绑定已验证邮箱");
        }
        emailVerificationService.send("RESET", target.getUserEmail(), target.getId());
        return ResultUtils.success(true);
    }

    /**
     * 用户登录（微信开放平台）
     */
    @GetMapping("/login/wx_open")
    public BaseResponse<LoginUserVO> userLoginByWxOpen(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("code") String code) {
        WxOAuth2AccessToken accessToken;
        try {
            WxMpService wxService = wxOpenConfig.getWxMpService();
            accessToken = wxService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxService.getOAuth2Service().getUserInfo(accessToken, code);
            String unionId = userInfo.getUnionId();
            String mpOpenId = userInfo.getOpenid();
            if (StringUtils.isAnyBlank(unionId, mpOpenId)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，系统错误");
            }
            return ResultUtils.success(userService.userLoginByMpOpen(userInfo, request));
        } catch (Exception e) {
            log.error("userLoginByWxOpen error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，系统错误");
        }
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {

        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        String encryptPassword = com.poj.poj.security.Passwords.encode(userAddRequest.getUserPassword());
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminUserVO> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        AdminUserVO result = new AdminUserVO();
        BeanUtils.copyProperties(user, result);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        if (id <= 0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AdminUserVO>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        if (userQueryRequest == null || userQueryRequest.getCurrent() < 1
                || userQueryRequest.getPageSize() < 1 || userQueryRequest.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<AdminUserVO> result = new Page<>(current, size, userPage.getTotal());
        result.setRecords(userPage.getRecords().stream().map(user -> {
            AdminUserVO item = new AdminUserVO();
            BeanUtils.copyProperties(user, item);
            return item;
        }).collect(java.util.stream.Collectors.toList()));
        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
            HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.length(userUpdateMyRequest.getUserProfile()) > 512
                || StringUtils.length(userUpdateMyRequest.getUserAvatar()) > 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人资料内容过长");
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.updateMyUser(userUpdateMyRequest, loginUser));
    }

    /** 上传并立即保存当前用户头像；本地开发不依赖腾讯云 COS。 */
    @PostMapping("/avatar")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile file,
                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String oldAvatar = loginUser.getUserAvatar();
        String avatarUrl = avatarStorageService.save(file, loginUser.getId());
        User update = new User();
        update.setId(loginUser.getId());
        update.setUserAvatar(avatarUrl);
        if (!userService.updateById(update)) {
            avatarStorageService.deleteIfLocal(avatarUrl);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像更新失败");
        }
        avatarStorageService.deleteIfLocal(oldAvatar);
        return ResultUtils.success(avatarUrl);
    }
}
