package com.poj.poj.exception;

import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public BaseResponse<?> duplicateKey(org.springframework.dao.DuplicateKeyException error) {
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "账号已存在");
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.warn("Business rejection: code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
