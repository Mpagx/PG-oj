package com.poj.poj.judge;

import cn.hutool.json.JSONUtil;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.judge.codesandbox.CodeSandbox;
import com.poj.poj.judge.codesandbox.CodeSandboxFactory;
import com.poj.poj.judge.codesandbox.CodeSandboxProxy;
import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import com.poj.poj.judge.strategy.JudgeContext;
import com.poj.poj.model.dto.question.JudgeCase;
import com.poj.poj.model.dto.question.JudgeConfig;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.enums.QuestionSubmitStatusEnum;
import com.poj.poj.model.enums.JudgeInfoMessageEnum;
import com.poj.poj.service.QuestionService;
import com.poj.poj.service.QuestionSubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

//    @Resource
//    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;
    @Autowired
    private JudgeManager judgeManager;

    @Autowired
    private RemoteCodeSandbox remoteCodeSandbox;

    @Autowired
    private QuestionSubmitStateMachine stateMachine;


    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2）通过条件更新抢占任务，防止并发执行同一提交。
        if (!stateMachine.tryStart(questionSubmitId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }
        // 4）调用沙箱，获取到执行结果
        CodeSandbox codeSandbox = "remote".equals(type)
                ? remoteCodeSandbox : CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        JudgeConfig judgeConfig = JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class);
        if (judgeConfig == null || judgeConfig.getTimeLimit() == null
                || judgeConfig.getMemoryLimit() == null || judgeConfig.getStackLimit() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目判题限制配置无效");
        }
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .protocolVersion("1.0")
                .requestId("question-submit-" + questionSubmitId)
                .code(code)
                .language(language)
                .inputList(inputList)
                .timeLimitMs(judgeConfig.getTimeLimit())
                .memoryLimitKb(judgeConfig.getMemoryLimit())
                .stackLimitKb(judgeConfig.getStackLimit())
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        if ("2".equals(executeCodeResponse.getStatus())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "代码沙箱执行异常：" + executeCodeResponse.getMessage());
        }
        List<String> outputList = executeCodeResponse.getOutputList();
        if ("3".equals(executeCodeResponse.getStatus())) {
            JudgeInfo userCodeFailure = executeCodeResponse.getJudgeInfo();
            if (userCodeFailure == null) {
                userCodeFailure = new JudgeInfo();
            }
            String detail = executeCodeResponse.getMessage();
            if (detail == null && userCodeFailure.getMessage() != null) {
                detail = userCodeFailure.getMessage();
            }
            if (detail != null && detail.contains("编译")) {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getValue());
            } else if (detail != null && detail.contains("超时")) {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            } else {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getValue());
            }
            userCodeFailure.setPassedCaseCount(outputList == null ? 0 : outputList.size());
            userCodeFailure.setTotalCaseCount(judgeCaseList.size());
            userCodeFailure.setDetail(detail);
            if (!stateMachine.complete(questionSubmitId, questionId, userCodeFailure, false)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
            }
            return questionSubmitService.getById(questionSubmitId);
        }
        // 5）根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);


        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        // 6）修改数据库中的判题结果
        judgeInfo.setPassedCaseCount("Accepted".equals(judgeInfo.getMessage())
                ? judgeCaseList.size() : 0);
        judgeInfo.setTotalCaseCount(judgeCaseList.size());
        judgeInfo.setDetail(executeCodeResponse.getMessage());
        boolean accepted = "Accepted".equals(judgeInfo.getMessage());
        if (!stateMachine.complete(questionSubmitId, questionId, judgeInfo, accepted)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionSubmitId);
        return questionSubmitResult;
    }
}
