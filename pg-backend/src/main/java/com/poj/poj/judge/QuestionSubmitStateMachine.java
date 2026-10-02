package com.poj.poj.judge;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.mapper.QuestionMapper;
import com.poj.poj.mapper.QuestionSubmitMapper;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.enums.QuestionSubmitStatusEnum;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class QuestionSubmitStateMachine {

    private final QuestionSubmitMapper questionSubmitMapper;

    private final QuestionMapper questionMapper;

    public QuestionSubmitStateMachine(QuestionSubmitMapper questionSubmitMapper,
                                      QuestionMapper questionMapper) {
        this.questionSubmitMapper = questionSubmitMapper;
        this.questionMapper = questionMapper;
    }

    public boolean tryStart(long submissionId) {
        return transition(submissionId, QuestionSubmitStatusEnum.WAITING,
                QuestionSubmitStatusEnum.RUNNING, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean complete(long submissionId, long questionId, JudgeInfo judgeInfo, boolean accepted) {
        boolean updated = transition(submissionId, QuestionSubmitStatusEnum.RUNNING,
                QuestionSubmitStatusEnum.SUCCEED, judgeInfo);
        if (updated && accepted && questionMapper.incrementAcceptedNum(questionId) != 1) {
            throw new IllegalStateException("题目通过数更新失败");
        }
        return updated;
    }

    public boolean fail(long submissionId, JudgeInfo judgeInfo) {
        return transition(submissionId, QuestionSubmitStatusEnum.RUNNING,
                QuestionSubmitStatusEnum.FAILED, judgeInfo);
    }

    private boolean transition(long submissionId,
                               QuestionSubmitStatusEnum expected,
                               QuestionSubmitStatusEnum target,
                               JudgeInfo judgeInfo) {
        LambdaUpdateWrapper<QuestionSubmit> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(QuestionSubmit::getId, submissionId)
                .eq(QuestionSubmit::getStatus, expected.getValue())
                .eq(QuestionSubmit::getIsDelete, 0)
                .set(QuestionSubmit::getStatus, target.getValue());
        if (judgeInfo != null) {
            wrapper.set(QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(judgeInfo));
        }
        return questionSubmitMapper.update(null, wrapper) == 1;
    }
}
