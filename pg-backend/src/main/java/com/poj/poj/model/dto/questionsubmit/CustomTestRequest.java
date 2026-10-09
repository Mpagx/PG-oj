package com.poj.poj.model.dto.questionsubmit;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomTestRequest implements Serializable {
    private Long questionId;
    private String language;
    private String code;
    private String input;

    private static final long serialVersionUID = 1L;
}
