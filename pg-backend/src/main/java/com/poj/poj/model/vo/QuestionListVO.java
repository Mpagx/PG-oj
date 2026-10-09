package com.poj.poj.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class QuestionListVO implements Serializable {
    private Long id;
    private String name;
    private Long questionCount;
    private Boolean containsQuestion;
    private Date createTime;
    private Date updateTime;
    private static final long serialVersionUID = 1L;
}
