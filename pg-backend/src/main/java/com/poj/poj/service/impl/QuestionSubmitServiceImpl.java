package com.poj.poj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.constant.CommonConstant;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.judge.QuestionSubmitCreatedEvent;
import com.poj.poj.mapper.QuestionMapper;
import com.poj.poj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.poj.poj.model.dto.questionsubmit.CustomTestRequest;
import com.poj.poj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.enums.QuestionSubmitLanguageEnum;
import com.poj.poj.model.enums.QuestionSubmitStatusEnum;
import com.poj.poj.model.vo.QuestionSubmitVO;
import com.poj.poj.model.vo.CustomTestResultVO;
import com.poj.poj.model.vo.QuestionVO;
import com.poj.poj.model.vo.UserSubmissionOverviewVO;
import com.poj.poj.service.QuestionService;
import com.poj.poj.service.QuestionSubmitService;
import com.poj.poj.mapper.QuestionSubmitMapper;
import com.poj.poj.service.UserService;
import com.poj.poj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Legion
* @description 针对表【question_submit(题目提交)】的数据库操作Service实现
* @createDate 2026-08-11 13:55:43
*/
@Slf4j
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
    implements QuestionSubmitService{
    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox remoteCodeSandbox;

    @org.springframework.beans.factory.annotation.Value("${codesandbox.type:remote}")
    private String sandboxType;
    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        String code = questionSubmitAddRequest.getCode();
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码不能为空");
        }
        if (code.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65536) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码长度不能超过 65536 个字符");
        }
        if ("java".equals(language)) {
            com.poj.poj.judge.JavaSubmissionValidator.validate(code);
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!"PUBLISHED".equals(question.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目尚未发布，暂不能提交");
        }
        com.poj.poj.judge.QuestionJudgeValidator.validate(question.getJudgeCase(), question.getJudgeConfig());
        // Do not create submissions that will immediately exhaust retries when Docker is offline.
        if ("remote".equals(sandboxType)) {
            com.poj.poj.judge.codesandbox.model.SandboxHealthResponse health = remoteCodeSandbox.health();
            if (health == null || !"UP".equals(health.getStatus())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "判题服务未就绪，请检查沙箱服务、Docker、执行镜像及 SSH 隧道；未创建提交，请稍后重试");
            }
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(code);
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        if (questionMapper.incrementSubmitNum(questionId) != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目提交数更新失败");
        }
        eventPublisher.publishEvent(new QuestionSubmitCreatedEvent(questionSubmitId));
        return questionSubmitId;
    }

    @Override
    public CustomTestResultVO runCustomTest(CustomTestRequest request, User loginUser) {
        String language = request.getLanguage();
        if (QuestionSubmitLanguageEnum.getEnumByValue(language) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        String code = request.getCode();
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码不能为空");
        }
        if (code.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65536) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码长度不能超过 65536 字节");
        }
        String input = request.getInput() == null ? "" : request.getInput();
        if (input.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65536) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "自定义输入不能超过 65536 字节");
        }
        if ("java".equals(language)) {
            com.poj.poj.judge.JavaSubmissionValidator.validate(code);
        }
        Question question = questionService.getById(request.getQuestionId());
        if (question == null || !"PUBLISHED".equals(question.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在或尚未发布");
        }
        com.poj.poj.model.dto.question.JudgeConfig config;
        try {
            config = cn.hutool.json.JSONUtil.toBean(question.getJudgeConfig(),
                    com.poj.poj.model.dto.question.JudgeConfig.class);
        } catch (RuntimeException error) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目资源限制配置无效");
        }
        com.poj.poj.judge.QuestionJudgeValidator.validateConfig(question.getJudgeConfig());
        if ("remote".equals(sandboxType)) {
            com.poj.poj.judge.codesandbox.model.SandboxHealthResponse health = remoteCodeSandbox.health();
            if (health == null || !"UP".equals(health.getStatus())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题服务未就绪，请稍后重试");
            }
        }
        com.poj.poj.judge.codesandbox.CodeSandbox sandbox = "remote".equals(sandboxType)
                ? remoteCodeSandbox
                : com.poj.poj.judge.codesandbox.CodeSandboxFactory.newInstance(sandboxType);
        sandbox = new com.poj.poj.judge.codesandbox.CodeSandboxProxy(sandbox);
        com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse response = sandbox.executeCode(
                com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest.builder()
                        .protocolVersion("1.0")
                        .requestId("custom-test-" + loginUser.getId() + "-" + java.util.UUID.randomUUID())
                        .inputList(Collections.singletonList(input))
                        .code(code)
                        .language(language)
                        .timeLimitMs(config.getTimeLimit())
                        .memoryLimitKb(config.getMemoryLimit())
                        .stackLimitKb(config.getStackLimit())
                        .build());
        if (response == null || "2".equals(response.getStatus())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    response == null ? "判题服务没有返回结果" : response.getMessage());
        }
        CustomTestResultVO result = new CustomTestResultVO();
        result.setOutput(response.getOutputList() == null || response.getOutputList().isEmpty()
                ? "" : response.getOutputList().get(0));
        result.setVerdict(response.getVerdict());
        result.setMessage(response.getMessage());
        if (response.getJudgeInfo() != null) {
            result.setTime(response.getJudgeInfo().getTime());
            result.setMemory(response.getJudgeInfo().getMemory());
        }
        return result;
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        long userId = loginUser.getId();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !userService.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        Set<Long> questionIds = questionSubmitList.stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questions = questionService.listByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> {
                    QuestionSubmitVO vo = getQuestionSubmitVO(questionSubmit, loginUser);
                    Question question = questions.get(questionSubmit.getQuestionId());
                    if (question != null) {
                        QuestionVO questionVO = new QuestionVO();
                        questionVO.setId(question.getId());
                        questionVO.setTitle(question.getTitle());
                        vo.setQuestionVO(questionVO);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }

    @Override
    public UserSubmissionOverviewVO getUserSubmissionOverview(long userId) {
        UserSubmissionOverviewVO overview = baseMapper.selectUserOverview(userId);
        if (overview == null) {
            overview = new UserSubmissionOverviewVO();
        }
        overview.setActivity(baseMapper.selectRecentActivity(userId));
        return overview;
    }


}






