package com.poj.poj.judge;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.poj.poj.mapper.QuestionSubmitMapper;
import com.poj.poj.model.entity.QuestionSubmit;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;

@Component
public class JudgeMetrics {
    public JudgeMetrics(MeterRegistry registry, QuestionSubmitMapper mapper, RemoteCodeSandbox sandbox) {
        registry.gauge("poj.judge.waiting", mapper, m -> count(m, 0));
        registry.gauge("poj.judge.running", mapper, m -> count(m, 1));
        registry.gauge("poj.judge.system.failed", mapper, m -> count(m, 3));
        registry.gauge("poj.sandbox.up", sandbox, s -> "UP".equals(s.health().getStatus()) ? 1 : 0);
    }
    private double count(QuestionSubmitMapper mapper, int status) {
        try { return mapper.selectCount(new QueryWrapper<QuestionSubmit>().eq("status", status).eq("isDelete", 0)); }
        catch (RuntimeException error) { return Double.NaN; }
    }
}
