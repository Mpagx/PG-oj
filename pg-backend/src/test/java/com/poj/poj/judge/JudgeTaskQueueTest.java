package com.poj.poj.judge;

import com.poj.poj.mapper.QuestionSubmitMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class JudgeTaskQueueTest {

    @Test
    void suppressesDuplicateInMemoryDelivery() {
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        JudgeTaskQueue queue = new JudgeTaskQueue(mock(JudgeService.class),
                mock(QuestionSubmitMapper.class), mock(QuestionSubmitStateMachine.class),
                executor, 10, 3, true);

        assertTrue(queue.enqueue(100L));
        assertFalse(queue.enqueue(100L));
    }

    @Test
    void keepsDatabaseTaskWhenMemoryQueueIsFull() {
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        doThrow(new TaskRejectedException("full")).when(executor).execute(any(Runnable.class));
        JudgeTaskQueue queue = new JudgeTaskQueue(mock(JudgeService.class),
                mock(QuestionSubmitMapper.class), mock(QuestionSubmitStateMachine.class),
                executor, 10, 3, true);

        assertFalse(queue.enqueue(101L));
        // 被拒绝后会移除本地去重标记，因此扫描器下一轮仍可重新投递。
        assertFalse(queue.enqueue(101L));
    }
}
