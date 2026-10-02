package com.poj.poj.judge.codesandbox;

import com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodeSandboxProxy implements CodeSandbox {

    private final CodeSandbox codeSandbox;

    public CodeSandboxProxy(CodeSandbox codeSandbox) {
        this.codeSandbox = codeSandbox;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("调用代码沙箱，requestId={}, language={}, caseCount={}",
                executeCodeRequest.getRequestId(), executeCodeRequest.getLanguage(),
                executeCodeRequest.getInputList() == null ? 0 : executeCodeRequest.getInputList().size());
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        if (executeCodeResponse == null) {
            throw new IllegalStateException("代码沙箱返回为空");
        }
        log.info("代码沙箱响应信息：{}", executeCodeResponse);
        return executeCodeResponse;
    }
}
