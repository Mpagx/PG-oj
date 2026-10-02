package com.poj.poj.judge;

import com.poj.poj.judge.strategy.DefaultJudgeStrategy;
import com.poj.poj.judge.strategy.JavaLanguageJudgeStrategy;
import com.poj.poj.judge.strategy.JudgeContext;
import com.poj.poj.judge.strategy.JudgeStrategy;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
