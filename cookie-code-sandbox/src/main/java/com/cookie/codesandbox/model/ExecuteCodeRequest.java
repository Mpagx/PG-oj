package com.cookie.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 执行代码请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    /** 协议版本。 */
    private String protocolVersion;

    /** 调用方生成的请求标识。 */
    private String requestId;

    /**
     * 输入用例列表
     */
    private List<String> inputList;

    /**
     * 代码
     */
    private String code;

    /**
     * 编程语言
     */
    private String language;

    /** 时间限制（毫秒）。 */
    private Long timeLimitMs;

    /** 内存限制（KiB）。 */
    private Long memoryLimitKb;

    /** 栈限制（KiB）。 */
    private Long stackLimitKb;
}
