package com.poj.poj.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.model.dto.question.JudgeConfig;
import com.poj.poj.model.enums.JudgeInfoMessageEnum;
import java.util.List;

public class DefaultJudgeStrategy implements JudgeStrategy {
    @Override
    public JudgeInfo doJudge(JudgeContext context) {
        JudgeInfo execution = context.getJudgeInfo();
        List<String> outputs = context.getOutputList();
        if (execution == null || execution.getTime() == null || execution.getMemory() == null
                || outputs == null) throw new IllegalStateException("沙箱执行数据缺失");
        JudgeInfo result = new JudgeInfo();
        result.setTime(execution.getTime());
        result.setMemory(execution.getMemory());
        int total = context.getJudgeCaseList().size();
        int passed = 0;
        for (int i = 0; i < Math.min(outputs.size(), total); i++) {
            if (normalize(context.getJudgeCaseList().get(i).getOutput()).equals(normalize(outputs.get(i)))) passed++;
        }
        result.setPassedCaseCount(passed);
        result.setTotalCaseCount(total);
        JudgeConfig config = JSONUtil.toBean(context.getQuestion().getJudgeConfig(), JudgeConfig.class);
        JudgeInfoMessageEnum verdict = JudgeInfoMessageEnum.ACCEPTED;
        if (execution.getMemory() > config.getMemoryLimit()) verdict = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
        else if (execution.getTime() > config.getTimeLimit()) verdict = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
        else if (outputs.size() != total || passed != total) verdict = JudgeInfoMessageEnum.WRONG_ANSWER;
        result.setMessage(verdict.getValue());
        return result;
    }
    /** Preserve leading spaces and internal blank lines. */
    public static String normalize(String output) {
        if (output == null) throw new IllegalStateException("沙箱输出缺失");
        return output.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[ \\t]+(?=\\n|$)", "").replaceAll("\\n+$", "");
    }
}
