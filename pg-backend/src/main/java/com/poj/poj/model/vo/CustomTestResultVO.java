package com.poj.poj.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomTestResultVO implements Serializable {
    private String output;
    private String verdict;
    private String message;
    private Long time;
    private Long memory;

    private static final long serialVersionUID = 1L;
}
