package com.poj.poj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.common.ResultUtils;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.poj.poj.model.dto.questionsubmit.CustomTestRequest;
import com.poj.poj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.QuestionSubmitVO;
import com.poj.poj.model.vo.CustomTestResultVO;
import com.poj.poj.model.vo.UserSubmissionOverviewVO;
import com.poj.poj.service.QuestionSubmitService;
import com.poj.poj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题目提交接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/question_submit")
@Slf4j
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserService userService;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的 id
     */
    @PostMapping("/")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null
                || questionSubmitAddRequest.getQuestionId() == null
                || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能点赞
        final User loginUser = userService.getLoginUser(request);
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(questionSubmitId);
    }

    /** 自定义测试只执行一组输入，不创建提交记录，也不影响题目统计。 */
    @PostMapping("/custom-test")
    public BaseResponse<CustomTestResultVO> runCustomTest(@RequestBody CustomTestRequest customTestRequest,
                                                           HttpServletRequest request) {
        if (customTestRequest == null || customTestRequest.getQuestionId() == null
                || customTestRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(questionSubmitService.runCustomTest(customTestRequest, loginUser));
    }

    /**
     * 根据 id 获取当前用户自己的提交结果。
     */
    @GetMapping("/get")
    public BaseResponse<QuestionSubmitVO> getQuestionSubmitById(@RequestParam long id,
                                                                 HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QuestionSubmit questionSubmit = questionSubmitService.getById(id);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!questionSubmit.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVO(questionSubmit, loginUser));
    }

    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        if (questionSubmitQueryRequest == null || questionSubmitQueryRequest.getCurrent() < 1
                || questionSubmitQueryRequest.getPageSize() < 1 || questionSubmitQueryRequest.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在 1 到 100 之间");
        }
        final User loginUser = userService.getLoginUser(request);
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }

    /** 当前登录用户的提交记录，禁止通过请求参数查看成其他用户。 */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listMyQuestionSubmissions(
            @RequestBody QuestionSubmitQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null || queryRequest.getCurrent() < 1
                || queryRequest.getPageSize() < 1 || queryRequest.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在 1 到 50 之间");
        }
        User loginUser = userService.getLoginUser(request);
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setSortField("createTime");
        queryRequest.setSortOrder("descend");
        Page<QuestionSubmit> page = questionSubmitService.page(
                new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                questionSubmitService.getQueryWrapper(queryRequest));
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(page, loginUser));
    }

    /** 当前登录用户的累计做题数据与最近 12 周活动。 */
    @GetMapping("/my/overview")
    public BaseResponse<UserSubmissionOverviewVO> getMySubmissionOverview(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(questionSubmitService.getUserSubmissionOverview(loginUser.getId()));
    }


}
