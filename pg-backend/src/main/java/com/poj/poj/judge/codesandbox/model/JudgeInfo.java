package com.poj.poj.judge.codesandbox.model;

import lombok.Data;

/**
 * 判题信息
 */

@Data
public class JudgeInfo {
    /**
 * 程序执行信息
 */
private String message;
    /**
     * 消耗内存
     */
private Long memory;
    /**
     * 消耗时间
     */
    private Long time;

    /** 已通过测试用例数。 */
    private Integer passedCaseCount;

    /** 测试用例总数。 */
    private Integer totalCaseCount;

    /** 面向提交者的结果详情。 */
    private String detail;


}
