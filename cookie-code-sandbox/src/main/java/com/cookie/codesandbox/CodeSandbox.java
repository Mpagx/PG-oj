package com.cookie.codesandbox;

import com.cookie.codesandbox.model.ExecuteCodeRequest;
import com.cookie.codesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {
    /**
     * 执行代码
     * 可以增加一个查看代码沙箱状态的接口
     *
     * @param
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
