package com.cookie.codesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 */
@Data
public class ExecuteMessage {

    private Integer exitValue;
    private String message;
    private String errorMessage;
    private String verdict;
    private Long time;

    private Long memory;
}
