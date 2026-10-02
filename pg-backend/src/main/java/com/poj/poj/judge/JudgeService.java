package com.poj.poj.judge;

import com.poj.poj.model.entity.QuestionSubmit;


/**
 * 判题服务
 */

public interface JudgeService {
    /**
     * 判题
     *
     * @param questionSubmitId
     * @return
     */
    QuestionSubmit doJudge(long questionSubmitId);
}
