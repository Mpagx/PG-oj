package com.poj.poj.model.dto.question;

import lombok.Data;

/**
 * 题目配置
 */
@Data
public class JudgeConfig {

    /**
     * 时间限制ms
     */
    private Long timeLimit;
    /**
     * 内存限制kb
     */
    private Long memoryLimit;

    /**
     * 堆栈限制kb
     */
    private Long stackLimit;
}
