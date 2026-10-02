package com.poj.poj.judge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.mapper.QuestionMapper;
import com.poj.poj.mapper.QuestionSubmitMapper;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.enums.QuestionSubmitStatusEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionSubmitStateMachineTest {

    @BeforeAll
    static void initializeMyBatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), QuestionSubmit.class);
    }

    @Test
    void allowsOnlyForwardTransitions() {
        assertTrue(QuestionSubmitStatusEnum.WAITING.canTransitionTo(QuestionSubmitStatusEnum.RUNNING));
        assertTrue(QuestionSubmitStatusEnum.RUNNING.canTransitionTo(QuestionSubmitStatusEnum.SUCCEED));
        assertTrue(QuestionSubmitStatusEnum.RUNNING.canTransitionTo(QuestionSubmitStatusEnum.FAILED));
        assertFalse(QuestionSubmitStatusEnum.WAITING.canTransitionTo(QuestionSubmitStatusEnum.SUCCEED));
    }

    @Test
    void usesAtomicUpdateAndCountsAcceptedResult() {
        QuestionSubmitMapper submitMapper = mock(QuestionSubmitMapper.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(submitMapper.update(any(), any())).thenReturn(1);
        when(questionMapper.incrementAcceptedNum(20L)).thenReturn(1);
        QuestionSubmitStateMachine stateMachine = new QuestionSubmitStateMachine(submitMapper, questionMapper);

        assertTrue(stateMachine.tryStart(10L));
        assertTrue(stateMachine.complete(10L, 20L, new JudgeInfo(), true));
        verify(questionMapper).incrementAcceptedNum(20L);
    }
}
