package com.poj.poj.judge;

import cn.hutool.json.JSONUtil;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.model.dto.question.JudgeCase;
import com.poj.poj.model.dto.question.JudgeConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Validate problem data before it can enter the execution queue. */
public final class QuestionJudgeValidator {
    private QuestionJudgeValidator() { }
    public static void validate(String casesJson, String configJson) {
        validateCases(casesJson);
        validateConfig(configJson);
    }
    public static void validateCases(String json) {
        try {
            if (json == null || !JSONUtil.isTypeJSONArray(json)) fail("判题用例必须是 JSON 数组");
            List<JudgeCase> cases = JSONUtil.toList(json, JudgeCase.class);
            if (cases.isEmpty() || cases.size() > 50) fail("判题用例数量必须为 1 到 50");
            int total = 0;
            for (JudgeCase item : cases) {
                if (item == null || item.getInput() == null || item.getOutput() == null) fail("每个用例必须包含 input 和 output，允许空字符串");
                int bytes = item.getInput().getBytes(StandardCharsets.UTF_8).length;
                total += bytes;
                if (bytes > 16384 || total > 262144) fail("判题输入超过沙箱大小限制");
                if (item.getOutput().getBytes(StandardCharsets.UTF_8).length > 1048576) fail("期望输出过长");
            }
        } catch (BusinessException e) { throw e;
        } catch (RuntimeException e) { fail("判题用例格式无效"); }
    }
    public static void validateConfig(String json) {
        try {
            if (json == null || !JSONUtil.isTypeJSONObject(json)) fail("必须填写判题资源配置");
            JudgeConfig config = JSONUtil.toBean(json, JudgeConfig.class);
            range("时间(ms)", config.getTimeLimit(), 100, 10000);
            range("内存(KB)", config.getMemoryLimit(), 16384, 524288);
            range("栈(KB)", config.getStackLimit(), 256, 65536);
        } catch (BusinessException e) { throw e;
        } catch (RuntimeException e) { fail("判题资源配置格式无效"); }
    }
    private static void range(String name, Long value, long min, long max) {
        if (value == null || value < min || value > max) fail(name + "必须在 " + min + " 到 " + max + " 之间");
    }
    private static void fail(String message) { throw new BusinessException(ErrorCode.PARAMS_ERROR, message); }
}
