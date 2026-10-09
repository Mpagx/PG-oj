package com.cookie.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 执行代码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    private String protocolVersion;

    private String requestId;

    /**
     * 输出列表
     */
    private List<String> outputList;

    /**
     * 接口信息
     */
    private String message;

    /**
     * 执行状态
     */
    private String status;
    private String verdict;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;
}
