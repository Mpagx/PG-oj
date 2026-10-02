package com.poj.poj.judge.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    private String protocolVersion;

    private String requestId;
    private List<String> inputList;
    private String code;
    private String language;

    private Long timeLimitMs;

    private Long memoryLimitKb;

    private Long stackLimitKb;
}
